import sqlite3
import sys

DUMP = sys.argv[1] if len(sys.argv) > 1 else 'dump.txt'

sql = {}
queries = []
for line in open(DUMP, encoding='utf-8').read().split('\n'):
    if not line:
        continue
    parts = line.split('\t')
    if parts[0] == 'SQL':
        sql[parts[1]] = parts[2]
    else:
        queries.append((parts[1], parts[2], parts[3:]))

passed = failed = 0
def check(cond, label):
    global passed, failed
    if cond: passed += 1; print("  PASS ", label)
    else: failed += 1; print("  FAIL ", label)

db = sqlite3.connect(':memory:')
db.execute(sql['CREATE_TABLE']); db.execute(sql['CREATE_INDEX_UUID']); db.execute(sql['CREATE_INDEX_REMOTE'])
# running the schema twice must be harmless (onUpgrade calls it again)
db.execute(sql['CREATE_TABLE']); db.execute(sql['CREATE_INDEX_UUID']); db.execute(sql['CREATE_INDEX_REMOTE'])
check(True, 'schema created, and re-running it is harmless (IF NOT EXISTS)')

def insert(user, uuid, name='Item', cat='shirt', color='Red', price=10.0, date='2026-01-01',
           status='pending_create', remote=None, version=1):
    cur = db.execute("INSERT INTO clothing_items (user_id, remote_id, client_uuid, name, category, color, price, photo_url, date_added, sync_status, version) "
                     "VALUES (?,?,?,?,?,?,?,NULL,?,?,?)", (user, remote, uuid, name, cat, color, price, date, status, version))
    return cur.lastrowid

def row(local_id):
    return db.execute("SELECT remote_id, sync_status, version, name FROM clothing_items WHERE local_id=?", (local_id,)).fetchone()

# ---------------------------------------------------------------- constraints
a = insert(1, 'uuid-a')
try:
    insert(1, 'uuid-a'); dup = False
except sqlite3.IntegrityError:
    dup = True
check(dup, 'same user + same client_uuid is refused (unique index)')
b = insert(2, 'uuid-a')
check(b is not None, 'another user may use the same client_uuid')
check(row(a)[0] is None, 'remote_id is NULL for a new item')

# ---------------------------------------------------------------- local edits
db.execute(sql['UPDATE_LOCAL'], ('Renamed', 'jacket', 'Black', 99.5, '2026-02-02', a, 1))
check(row(a) == (None, 'pending_create', 2, 'Renamed'), 'edit of a never-synced item: stays pending_create, version 2')
s = insert(1, 'uuid-s', status='synced', remote=50)
db.execute(sql['UPDATE_LOCAL'], ('Edited', 'shirt', 'Red', 1.0, '2026-01-01', s, 1))
check(row(s) == (50, 'pending_update', 2, 'Edited'), 'edit of a synced item: becomes pending_update, version 2')
db.execute(sql['UPDATE_LOCAL'], ('Edited again', 'shirt', 'Red', 1.0, '2026-01-01', s, 1))
check(row(s) == (50, 'pending_update', 3, 'Edited again'), 'second edit: still pending_update, version 3')
db.execute(sql['UPDATE_LOCAL'], ('Hack', 'shirt', 'Red', 1.0, '2026-01-01', s, 2))
check(row(s)[3] == 'Edited again', "another user's edit does not touch the row")
d = insert(1, 'uuid-d', status='synced', remote=51)
db.execute(sql['MARK_DELETED'], (d, 1))
check(row(d) == (51, 'pending_delete', 2, 'Item'), 'delete: pending_delete (hidden), version 2')
db.execute(sql['UPDATE_LOCAL'], ('Ghost edit', 'shirt', 'Red', 1.0, '2026-01-01', d, 1))
check(row(d)[3] == 'Item', 'a deleted row can no longer be edited')
db.execute(sql['MARK_DELETED'], (d, 2))
check(row(d)[2] == 2, 'deleting twice does not bump the version again')
db.execute(sql['MARK_DELETED'], (a, 1))
check(row(a)[1] == 'pending_delete' and row(a)[0] is None, 'delete of a never-synced item: pending_delete, no remote id')

# ---------------------------------------------------------------- MARK_CREATED
c1 = insert(1, 'uuid-c1')
db.execute(sql['MARK_CREATED'], (101, 1, c1))
check(row(c1)[:3] == (101, 'synced', 1), 'create pushed, no edit meanwhile -> synced')
c2 = insert(1, 'uuid-c2')
db.execute(sql['UPDATE_LOCAL'], ('Edited during sync', 'shirt', 'Red', 1.0, '2026-01-01', c2, 1))   # version 2
db.execute(sql['MARK_CREATED'], (102, 1, c2))                                                      # engine sent version 1
check(row(c2)[:3] == (102, 'pending_update', 2), 'edited WHILE the create was in flight -> pending_update (will be pushed again)')
c3 = insert(1, 'uuid-c3')
db.execute(sql['MARK_DELETED'], (c3, 1))
db.execute(sql['MARK_CREATED'], (103, 1, c3))
check(row(c3)[:2] == (103, 'pending_delete'), 'deleted WHILE the create was in flight -> stays pending_delete, now with its remote id')

# ---------------------------------------------------------------- MARK_UPDATED
u1 = insert(1, 'uuid-u1', status='pending_update', remote=201, version=3)
db.execute(sql['MARK_UPDATED'], (u1, 3))
check(row(u1)[1] == 'synced', 'update pushed, no edit meanwhile -> synced')
u2 = insert(1, 'uuid-u2', status='pending_update', remote=202, version=4)
db.execute(sql['MARK_UPDATED'], (u2, 3))
check(row(u2)[1] == 'pending_update', 'edited while the update was in flight -> stays pending_update')
u3 = insert(1, 'uuid-u3', status='pending_delete', remote=203, version=4)
db.execute(sql['MARK_UPDATED'], (u3, 4))
check(row(u3)[1] == 'pending_delete', 'MARK_UPDATED never resurrects a deleted row')

# ---------------------------------------------------------------- LINK_REMOTE
l1 = insert(1, 'uuid-l1')
db.execute(sql['LINK_REMOTE'], (301, l1))
check(row(l1)[:2] == (301, 'pending_update'), 'found on the server by uuid: remote id stored, our data is pushed next')
l2 = insert(1, 'uuid-l2', status='pending_delete')
db.execute(sql['LINK_REMOTE'], (302, l2))
check(row(l2)[:2] == (302, 'pending_delete'), 'link keeps a pending_delete row pending_delete')

# ---------------------------------------------------------------- OVERWRITE_FROM_REMOTE
o1 = insert(1, 'uuid-o1', status='synced', remote=401, name='Old name', version=1)
db.execute(sql['OVERWRITE_FROM_REMOTE'], (401, 'Server name', 'shoes', 'Blue', 77.0, None, '2026-03-03', o1, 1))
r = db.execute("SELECT name, category, color, price, date_added, sync_status FROM clothing_items WHERE local_id=?", (o1,)).fetchone()
check(r == ('Server name', 'shoes', 'Blue', 77.0, '2026-03-03', 'synced'), "synced row takes the server's values")
o2 = insert(1, 'uuid-o2', status='pending_update', remote=402, name='My edit', version=2)
db.execute(sql['OVERWRITE_FROM_REMOTE'], (402, 'Server name', 'shoes', 'Blue', 77.0, None, '2026-03-03', o2, 2))
check(row(o2)[3] == 'My edit', 'a row with a pending local change is NOT overwritten')
o3 = insert(1, 'uuid-o3', status='synced', remote=403, name='Before', version=1)
db.execute(sql['UPDATE_LOCAL'], ('Edited meanwhile', 'shirt', 'Red', 1.0, '2026-01-01', o3, 1))
db.execute(sql['OVERWRITE_FROM_REMOTE'], (403, 'Server name', 'shoes', 'Blue', 77.0, None, '2026-03-03', o3, 1))
check(row(o3)[3] == 'Edited meanwhile', 'edited while the pull was running -> not overwritten')

# ---------------------------------------------------------------- reading
db.execute("DELETE FROM clothing_items")
x = insert(1, 'x1', status='synced', remote=1); y = insert(1, 'x2'); z = insert(1, 'x3', status='pending_delete', remote=3); w = insert(2, 'x4')
count = lambda q, u: db.execute(sql[q], (u,)).fetchone()[0]
check(count('COUNT_VISIBLE', 1) == 2, 'COUNT_VISIBLE hides pending_delete rows and other users')
check(count('COUNT_PENDING', 1) == 2, 'COUNT_PENDING = create + delete waiting (not the synced one)')
check(len(db.execute(sql['SELECT_ALL_ROWS'], (1,)).fetchall()) == 3, 'SELECT_ALL_ROWS (sync) includes pending_delete rows')
check(len(db.execute(sql['SELECT_VISIBLE'], (1,)).fetchall()) == 2, 'SELECT_VISIBLE hides them')
check(db.execute(sql['SELECT_ONE'], (z, 1)).fetchone() is None, 'SELECT_ONE does not return a deleted row')
check(db.execute(sql['SELECT_ONE'], (w, 1)).fetchone() is None, "SELECT_ONE does not return another user's row")
check(db.execute(sql['SELECT_ONE'], (x, 1)).fetchone() is not None, 'SELECT_ONE returns the user\'s own row')
# the column list matches what ClothingDao reads
cols = [d[0] for d in db.execute(sql['SELECT_ALL_ROWS'], (1,)).description]
check(cols == ['local_id', 'user_id', 'remote_id', 'client_uuid', 'name', 'category', 'color', 'price', 'photo_url', 'date_added', 'sync_status', 'version'],
      'column names match the ones ClothingDao reads: ' + ','.join(cols))

# ---------------------------------------------------------------- wardrobe screen query
db.execute("DELETE FROM clothing_items")
items = [('White Oxford Shirt', 'shirt', 'White', 65, '2026-09-02'), ('Navy Chinos', 'trousers', 'Navy', 89.9, '2026-09-10'),
         ('White Sneakers', 'shoes', 'White', 150, '2026-09-28'), ('Black Leather Jacket', 'jacket', 'Black', 240, '2026-08-21'),
         ('Grey Hoodie', 'shirt', 'Grey', 59.9, '2026-10-12'), ('Brown Leather Belt', 'accessory', 'Brown', 35, '2026-07-30'),
         ('100% cotton tee', 'shirt', 'Red', 20, '2026-10-01'), ('under_score', 'other', 'Green', 5, '2026-10-02'), ('back\\slash', 'other', 'Green', 6, '2026-10-03')]
for i, (n, ca, co, p, d) in enumerate(items):
    insert(1, 'q%d' % i, name=n, cat=ca, color=co, price=p, date=d, status='synced', remote=i + 1)
insert(1, 'qdel', name='Deleted thing', status='pending_delete', remote=99)
insert(2, 'qother', name='Other user shirt')

def run(label):
    for key, q, args in queries:
        if key.split('|')[0:5] and label == key:
            return db.execute(q, args).fetchall(), q
    raise KeyError(label)

names = lambda label: [r[4] for r in run(label)[0]]
check(len(run('1||null|null|0')[0]) == 9, 'no filter: 9 visible items (deleted + other user hidden)')
check(names('1||null|null|0')[:2] == ['Grey Hoodie', 'under_score'] or names('1||null|null|0')[0] in ('back\\slash', 'under_score', 'Grey Hoodie'), 'default sort starts with the newest')
dates = [r[9] for r in run('1||null|null|0')[0]]
check(dates == sorted(dates, reverse=True), 'sort newest: dates descending')
nm = names('1|null|null|null|1')
check(nm == sorted(nm, key=str.lower), 'sort name A-Z, case-insensitive')
pr = [r[7] for r in run('1|navy|null|null|0')[0]]
check(names('1|navy|null|null|0') == ['Navy Chinos'], 'search "navy" -> Navy Chinos')
check(sorted(names('1|white|null|null|1')) == ['White Oxford Shirt', 'White Sneakers'], 'search "white" matches name or color (case-insensitive)')
check([r[7] for r in run('1||shirt|null|2')[0]] == [20.0, 59.9, 65.0], 'category shirt + sort price low -> 20, 59.9, 65')
check([r[4] for r in run('1||null|White|3')[0]] == ['White Sneakers', 'White Oxford Shirt'], 'color White + sort price high')
check(names('1||shoes|White|0') == ['White Sneakers'], 'category + color combined')
check(names('1|100%|null|null|0') == ['100% cotton tee'], 'search "100%": % is a plain character (1 match, not everything)')
check(names('1|_|null|null|0') == ['under_score'], 'search "_": _ is a plain character')
check(names('1|a\\b|null|null|0') == ['back\\slash'] or len(names('1|a\\b|null|null|0')) <= 1, 'search with a backslash does not crash')
check(names("1|' OR '1'='1|null|null|0") == [], 'SQL injection text in search: no match, no error')
check(names("1|'; DROP TABLE clothing_items;--|null|null|0") == [], 'DROP TABLE attempt: nothing happens')
check(db.execute("SELECT COUNT(*) FROM clothing_items").fetchone()[0] == 11, '... and the table is still there')
check(len(run('2||null|null|0')[0]) == 1, 'user 2 sees only his own item')

print('\n%s' % ('ALL %d SQL CHECKS PASSED' % passed if not failed else '%d FAILED, %d passed' % (failed, passed)))
raise SystemExit(1 if failed else 0)
