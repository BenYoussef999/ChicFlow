import com.chicflow.tunisia.models.ClothingItem;
import com.chicflow.tunisia.network.*;
import com.chicflow.tunisia.sync.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class E2E {

    static int passed = 0, failed = 0;
    static String BASE;

    static void check(boolean ok, String label) {
        if (ok) { passed++; System.out.println("  PASS  " + label); }
        else { failed++; System.out.println("  FAIL  " + label); }
    }
    static void section(String t) { System.out.println("\n== " + t + " =="); }

    // ------------------------------------------------------------------ real HTTP transport
    static class HttpTransport implements Transport {
        final String token;
        HttpTransport(String token) { this.token = token; }
        public JSONObject get(String endpoint) throws ApiException { return request("GET", endpoint, null); }
        public JSONObject post(String endpoint, JSONObject body) throws ApiException { return request("POST", endpoint, body); }

        private JSONObject request(String method, String endpoint, JSONObject body) throws ApiException {
            HttpURLConnection c = null;
            try {
                c = (HttpURLConnection) new URL(BASE + endpoint).openConnection();
                c.setRequestMethod(method);
                c.setConnectTimeout(5000);
                c.setReadTimeout(20000);
                c.setRequestProperty("Accept", "application/json");
                if (token != null) c.setRequestProperty("Authorization", "Bearer " + token);
                if (body != null) {
                    c.setDoOutput(true);
                    c.setRequestProperty("Content-Type", "application/json");
                    try (OutputStream os = c.getOutputStream()) { os.write(body.toString().getBytes(StandardCharsets.UTF_8)); }
                }
                int status = c.getResponseCode();
                InputStream in = status >= 400 ? c.getErrorStream() : c.getInputStream();
                StringBuilder sb = new StringBuilder();
                try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = r.readLine()) != null) sb.append(line);
                }
                JSONObject json = new JSONObject(sb.toString());
                if (!json.optBoolean("success", false)) throw new ApiException(json.optString("message", "error"), status);
                JSONObject data = json.optJSONObject("data");
                return data != null ? data : new JSONObject();
            } catch (IOException | JSONException e) {
                throw new ApiException("network: " + e, -1);
            } finally {
                if (c != null) c.disconnect();
            }
        }
    }

    // ------------------------------------------------------------------ a "phone": in-memory twin of ClothingDao / ItemSql
    static class Phone implements LocalStore {
        static class Row { ClothingItem item; int userId; }
        final List<Row> rows = new ArrayList<>();
        int nextId = 1;

        static ClothingItem copy(ClothingItem i) {
            ClothingItem c = new ClothingItem(i.getId(), i.getName(), i.getCategory(), i.getColor(), i.getPrice(), i.getPhotoUrl(), i.getDateAdded());
            c.setRemoteId(i.getRemoteId()); c.setClientUuid(i.getClientUuid()); c.setSyncStatus(i.getSyncStatus()); c.setVersion(i.getVersion());
            return c;
        }
        Row find(int localId) { for (Row r : rows) if (r.item.getId() == localId) return r; return null; }

        // --- what the screens do (WardrobeRepository -> ClothingDao)
        int add(int userId, String name, String category, String color, double price, String date) {
            ClothingItem item = new ClothingItem(nextId++, name, category, color, price, null, date);
            item.setClientUuid(UUID.randomUUID().toString());
            item.setSyncStatus(SyncStatus.PENDING_CREATE);
            item.setVersion(1);
            Row r = new Row(); r.item = item; r.userId = userId; rows.add(r);
            return item.getId();
        }
        void edit(int localId, String name, double price) {          // UPDATE_LOCAL
            Row r = find(localId);
            if (r == null || SyncStatus.PENDING_DELETE.equals(r.item.getSyncStatus())) return;
            r.item.setName(name); r.item.setPrice(price);
            r.item.setVersion(r.item.getVersion() + 1);
            if (!SyncStatus.PENDING_CREATE.equals(r.item.getSyncStatus())) r.item.setSyncStatus(SyncStatus.PENDING_UPDATE);
        }
        void editCategory(int localId, String category) {
            Row r = find(localId);
            r.item.setCategory(category);
            r.item.setVersion(r.item.getVersion() + 1);
            if (!SyncStatus.PENDING_CREATE.equals(r.item.getSyncStatus())) r.item.setSyncStatus(SyncStatus.PENDING_UPDATE);
        }
        void delete(int localId) {                                    // MARK_DELETED
            Row r = find(localId);
            if (r == null || SyncStatus.PENDING_DELETE.equals(r.item.getSyncStatus())) return;
            r.item.setSyncStatus(SyncStatus.PENDING_DELETE);
            r.item.setVersion(r.item.getVersion() + 1);
        }
        List<ClothingItem> visible(int userId) {
            List<ClothingItem> out = new ArrayList<>();
            for (Row r : rows) if (r.userId == userId && !SyncStatus.PENDING_DELETE.equals(r.item.getSyncStatus())) out.add(copy(r.item));
            return out;
        }
        int pending(int userId) {
            int n = 0;
            for (Row r : rows) if (r.userId == userId && !SyncStatus.SYNCED.equals(r.item.getSyncStatus())) n++;
            return n;
        }
        ClothingItem byName(int userId, String name) {
            for (ClothingItem i : visible(userId)) if (name.equals(i.getName())) return i;
            return null;
        }
        ClothingItem raw(int localId) { Row r = find(localId); return r == null ? null : copy(r.item); }

        // --- what the sync engine does (LocalStore) : same rules as ItemSql
        public List<ClothingItem> getAllRows(int userId) {
            List<ClothingItem> out = new ArrayList<>();
            for (Row r : rows) if (r.userId == userId) out.add(copy(r.item));
            return out;
        }
        public void markCreated(int localId, int expectedVersion, int remoteId) {
            Row r = find(localId); if (r == null) return;
            r.item.setRemoteId(remoteId);
            if (r.item.getVersion() == expectedVersion) r.item.setSyncStatus(SyncStatus.SYNCED);
            else if (SyncStatus.PENDING_CREATE.equals(r.item.getSyncStatus())) r.item.setSyncStatus(SyncStatus.PENDING_UPDATE);
        }
        public void markUpdated(int localId, int expectedVersion) {
            Row r = find(localId); if (r == null) return;
            if (r.item.getVersion() == expectedVersion && SyncStatus.PENDING_UPDATE.equals(r.item.getSyncStatus())) r.item.setSyncStatus(SyncStatus.SYNCED);
        }
        public void linkToRemote(int localId, int remoteId) {
            Row r = find(localId); if (r == null) return;
            r.item.setRemoteId(remoteId);
            if (SyncStatus.PENDING_CREATE.equals(r.item.getSyncStatus())) r.item.setSyncStatus(SyncStatus.PENDING_UPDATE);
        }
        public void overwriteFromRemote(int localId, int expectedVersion, ClothingItem remote) {
            Row r = find(localId); if (r == null) return;
            if (r.item.getVersion() != expectedVersion || !SyncStatus.SYNCED.equals(r.item.getSyncStatus())) return;
            r.item.setRemoteId(remote.getRemoteId()); r.item.setName(remote.getName()); r.item.setCategory(remote.getCategory());
            r.item.setColor(remote.getColor()); r.item.setPrice(remote.getPrice()); r.item.setPhotoUrl(remote.getPhotoUrl()); r.item.setDateAdded(remote.getDateAdded());
        }
        public void insertFromRemote(int userId, ClothingItem remote) {
            ClothingItem item = copy(remote);
            item.setId(nextId++);
            if (item.getClientUuid() == null) item.setClientUuid(UUID.randomUUID().toString());
            item.setSyncStatus(SyncStatus.SYNCED); item.setVersion(1);
            Row r = new Row(); r.item = item; r.userId = userId; rows.add(r);
        }
        public void deleteRow(int localId) { Row r = find(localId); if (r != null) rows.remove(r); }
    }

    // ------------------------------------------------------------------ a network that misbehaves on demand
    static class Flaky implements RemoteApi {
        final RemoteApi real;
        boolean offline = false;
        int loseCreateResponses = 0;                  // create reaches the server, the answer is lost
        Runnable afterCreateOnServer = null;          // runs after the server created the item, before the engine hears about it
        Runnable afterUpdateOnServer = null;
        String refuseCreateOfName = null;             // pretend the server answers 422 for this item
        Flaky(RemoteApi real) { this.real = real; }

        void down() throws ApiException { if (offline) throw new ApiException("offline", -1); }

        public ClothingItem createItem(ClothingItem item) throws ApiException {
            down();
            if (item.getName().equals(refuseCreateOfName)) throw new ApiException("refused", 422);
            ClothingItem created = real.createItem(item);
            if (loseCreateResponses > 0) { loseCreateResponses--; throw new ApiException("connection lost", -1); }
            if (afterCreateOnServer != null) { Runnable r = afterCreateOnServer; afterCreateOnServer = null; r.run(); }
            return created;
        }
        public void updateItem(ClothingItem item) throws ApiException {
            down();
            real.updateItem(item);
            if (afterUpdateOnServer != null) { Runnable r = afterUpdateOnServer; afterUpdateOnServer = null; r.run(); }
        }
        public void deleteItem(int remoteId) throws ApiException { down(); real.deleteItem(remoteId); }
        public List<ClothingItem> pullItems() throws ApiException { down(); return real.pullItems(); }
    }

    // ------------------------------------------------------------------ test helpers
    static class User {
        final int id; final String token; final RestRemoteApi api;
        User(int id, String token) { this.id = id; this.token = token; this.api = new RestRemoteApi(new HttpTransport(token)); }
        List<ClothingItem> server() throws ApiException { return api.pullItems(); }
        ClothingItem serverByName(String name) throws ApiException {
            for (ClothingItem i : server()) if (name.equals(i.getName())) return i;
            return null;
        }
        int serverCountWithUuid(String uuid) throws ApiException {
            int n = 0; for (ClothingItem i : server()) if (uuid.equals(i.getClientUuid())) n++; return n;
        }
    }

    static User register(String label) throws Exception {
        JSONObject body = new JSONObject();
        body.put("name", label); body.put("email", label.toLowerCase().replace(' ', '_') + "_" + System.nanoTime() + "@example.com"); body.put("password", "secret123");
        JSONObject data = new HttpTransport(null).post("auth/register.php", body);
        return new User(data.getJSONObject("user").getInt("id"), data.getString("token"));
    }

    static SyncResult sync(Phone phone, User u, RemoteApi api) { return new SyncEngine(phone, api).sync(u.id); }
    static SyncResult sync(Phone phone, User u) { return sync(phone, u, u.api); }

    /** Phone and server hold exactly the same items (name, price, category, color, date). */
    static boolean sameAsServer(Phone phone, User u) throws ApiException {
        List<ClothingItem> local = phone.visible(u.id), server = u.server();
        if (local.size() != server.size()) return false;
        Map<Integer, ClothingItem> byRemote = new HashMap<>();
        for (ClothingItem s : server) byRemote.put(s.getRemoteId(), s);
        for (ClothingItem l : local) {
            ClothingItem s = byRemote.get(l.getRemoteId());
            if (s == null || !s.getName().equals(l.getName()) || s.getPrice() != l.getPrice()
                    || !s.getCategory().equals(l.getCategory()) || !s.getColor().equals(l.getColor())
                    || !s.getDateAdded().equals(l.getDateAdded())) return false;
        }
        return true;
    }

    // ================================================================== scenarios
    public static void main(String[] args) throws Exception {
        BASE = args[0].endsWith("/") ? args[0] : args[0] + "/";

        User alice = register("Alice Sync");
        User bob = register("Bob Sync");

        // -------------------------------------------------------------
        section("1. First sync: local items go to the server");
        Phone p1 = new Phone();
        int shirt = p1.add(alice.id, "White Oxford Shirt", "shirt", "White", 65, "2026-09-02");
        int chinos = p1.add(alice.id, "Navy Chinos", "trousers", "Navy", 89.9, "2026-09-10");
        int shoes = p1.add(alice.id, "White Sneakers", "shoes", "White", 150, "2026-09-28");
        check(p1.pending(alice.id) == 3 && alice.server().isEmpty(), "3 items waiting, server empty");
        SyncResult r = sync(p1, alice);
        check(r.completed && !r.offline && !r.authExpired && r.pushed == 3 && r.failed == 0, "sync completed, 3 items pushed");
        check(p1.pending(alice.id) == 0, "nothing pending any more");
        check(sameAsServer(p1, alice) && alice.server().size() == 3, "phone and server are identical (3 items)");
        check(p1.raw(shirt).getRemoteId() > 0 && p1.raw(shirt).getSyncStatus().equals(SyncStatus.SYNCED), "local rows got their server ids and are 'synced'");
        check(alice.serverCountWithUuid(p1.raw(shirt).getClientUuid()) == 1, "the client_uuid reached the server");
        int shirtLocalId = shirt;
        r = sync(p1, alice);
        check(r.completed && r.pushed == 0 && r.pulled == 0, "second sync with no changes does nothing (pushed 0, pulled 0)");

        // -------------------------------------------------------------
        section("2. Edit and delete on the phone");
        p1.edit(chinos, "Navy Chinos (slim)", 79.5);
        check(p1.raw(chinos).getSyncStatus().equals(SyncStatus.PENDING_UPDATE), "edit -> pending_update");
        r = sync(p1, alice);
        check(r.completed && r.pushed == 1 && sameAsServer(p1, alice) && alice.serverByName("Navy Chinos (slim)").getPrice() == 79.5, "edit reached the server");
        p1.delete(shoes);
        check(p1.visible(alice.id).size() == 2 && alice.server().size() == 3, "deleted item disappears from the screen at once, server still has it");
        r = sync(p1, alice);
        check(r.completed && alice.server().size() == 2 && p1.raw(shoes) == null, "delete reached the server and the row is gone");
        check(sameAsServer(p1, alice), "phone and server identical again");

        // -------------------------------------------------------------
        section("3. Second device (same account)");
        Phone p2 = new Phone();
        r = sync(p2, alice);
        check(r.completed && r.pulled == 2 && p2.visible(alice.id).size() == 2 && sameAsServer(p2, alice), "new phone downloads the 2 items");
        check(p2.visible(alice.id).get(0).getSyncStatus().equals(SyncStatus.SYNCED), "downloaded rows are 'synced'");

        p2.edit(p2.byName(alice.id, "White Oxford Shirt").getId(), "White Oxford Shirt v2", 70);
        sync(p2, alice);
        r = sync(p1, alice);
        check(r.pulled >= 1 && p1.raw(shirtLocalId).getName().equals("White Oxford Shirt v2") && p1.raw(shirtLocalId).getId() == shirtLocalId, "edit made on device 2 arrives on device 1 (same local id kept)");

        int fromP2 = p2.add(alice.id, "Grey Hoodie", "shirt", "Grey", 59.9, "2026-10-12");
        sync(p2, alice);
        r = sync(p1, alice);
        check(p1.byName(alice.id, "Grey Hoodie") != null && sameAsServer(p1, alice), "item created on device 2 appears on device 1");

        p2.delete(p2.byName(alice.id, "Grey Hoodie").getId());
        sync(p2, alice);
        r = sync(p1, alice);
        check(p1.byName(alice.id, "Grey Hoodie") == null && sameAsServer(p1, alice), "item deleted on device 2 disappears from device 1");

        // -------------------------------------------------------------
        section("4. Offline: nothing is lost");
        Flaky flaky = new Flaky(alice.api);
        flaky.offline = true;
        int cap = p1.add(alice.id, "Offline Cap", "accessory", "Green", 20, "2026-10-20");
        int belt = p1.add(alice.id, "Offline Belt", "accessory", "Brown", 35, "2026-10-21");
        p1.edit(shirtLocalId, "White Oxford Shirt v3", 72);
        p1.delete(chinos);
        r = sync(p1, alice, flaky);
        check(!r.completed && r.offline && !r.authExpired, "offline sync reports 'offline', not a crash");
        check(p1.pending(alice.id) == 4, "all 4 changes are still pending (2 creates, 1 edit, 1 delete)");
        check(p1.visible(alice.id).size() == 3 && p1.byName(alice.id, "Offline Cap") != null && p1.byName(alice.id, "Navy Chinos (slim)") == null,
                "the app keeps working offline: screens show the local state");
        r = sync(p1, alice, flaky);
        check(!r.completed && p1.pending(alice.id) == 4, "still offline: still 4 pending, repeated attempts are harmless");
        flaky.offline = false;
        r = sync(p1, alice, flaky);
        check(r.completed && r.pushed == 4 && p1.pending(alice.id) == 0, "back online: all 4 changes pushed");
        check(sameAsServer(p1, alice), "phone and server identical after reconnecting");

        // -------------------------------------------------------------
        section("5. Lost response (the server saved the item, the phone never heard)");
        flaky = new Flaky(alice.api);
        flaky.loseCreateResponses = 1;
        int lost = p1.add(alice.id, "Lost Response Scarf", "accessory", "Red", 15, "2026-10-22");
        String lostUuid = p1.raw(lost).getClientUuid();
        r = sync(p1, alice, flaky);
        check(!r.completed && r.offline, "sync reports failure");
        check(alice.serverCountWithUuid(lostUuid) == 1, "...but the server DID create the item");
        check(p1.raw(lost).getSyncStatus().equals(SyncStatus.PENDING_CREATE) && p1.raw(lost).getRemoteId() == 0, "the phone still thinks it is pending_create");
        r = sync(p1, alice, flaky);
        check(r.completed, "retry completes");
        check(alice.serverCountWithUuid(lostUuid) == 1, "NO DUPLICATE on the server (client_uuid made the retry safe)");
        check(p1.raw(lost).getSyncStatus().equals(SyncStatus.SYNCED) && p1.raw(lost).getRemoteId() > 0 && sameAsServer(p1, alice), "phone now synced with the existing server item");

        section("6. Lost response, then the user deletes the item before the retry");
        flaky = new Flaky(alice.api);
        flaky.loseCreateResponses = 1;
        int ghost = p1.add(alice.id, "Ghost Gloves", "accessory", "Black", 12, "2026-10-23");
        String ghostUuid = p1.raw(ghost).getClientUuid();
        sync(p1, alice, flaky);                                   // server has it, phone does not know
        check(alice.serverCountWithUuid(ghostUuid) == 1, "server has the item");
        p1.delete(ghost);
        r = sync(p1, alice);
        check(r.completed && alice.serverCountWithUuid(ghostUuid) == 0, "the item is deleted on the server too (no ghost left)");
        check(p1.raw(ghost) == null && sameAsServer(p1, alice), "local row removed, phone = server");

        // -------------------------------------------------------------
        section("7. Edits made WHILE a sync is running");
        flaky = new Flaky(alice.api);
        int racer = p1.add(alice.id, "Race Jacket", "jacket", "Black", 200, "2026-10-24");
        flaky.afterCreateOnServer = () -> p1.edit(racer, "Race Jacket EDITED", 210);
        r = sync(p1, alice, flaky);
        check(r.completed, "sync completes");
        check(p1.raw(racer).getSyncStatus().equals(SyncStatus.PENDING_UPDATE) && p1.raw(racer).getRemoteId() > 0, "item edited during its create stays pending_update (the edit is not lost)");
        check(alice.serverByName("Race Jacket") != null && p1.raw(racer).getName().equals("Race Jacket EDITED"), "server has the old name, phone the new one");
        r = sync(p1, alice);
        check(r.completed && alice.serverByName("Race Jacket EDITED") != null && alice.serverByName("Race Jacket") == null, "next sync pushes the edit");
        check(p1.raw(racer).getSyncStatus().equals(SyncStatus.SYNCED) && sameAsServer(p1, alice), "synced and identical");

        flaky = new Flaky(alice.api);
        p1.edit(racer, "Race Jacket v2", 220);
        flaky.afterUpdateOnServer = () -> p1.edit(racer, "Race Jacket v3", 230);
        sync(p1, alice, flaky);
        check(p1.raw(racer).getSyncStatus().equals(SyncStatus.PENDING_UPDATE) && p1.raw(racer).getName().equals("Race Jacket v3"), "edit during an update push stays pending");
        sync(p1, alice);
        check(alice.serverByName("Race Jacket v3") != null && sameAsServer(p1, alice), "...and is pushed next time");

        // -------------------------------------------------------------
        section("8. The server refuses one change (422)");
        flaky = new Flaky(alice.api);
        int good1 = p1.add(alice.id, "Good Tee One", "shirt", "Red", 10, "2026-10-25");
        int bad = p1.add(alice.id, "Bad Tee", "shirt", "Red", 10, "2026-10-25");
        int good2 = p1.add(alice.id, "Good Tee Two", "shirt", "Red", 10, "2026-10-25");
        flaky.refuseCreateOfName = "Bad Tee";
        r = sync(p1, alice, flaky);
        check(r.completed && r.failed == 1 && r.pushed == 2, "completed: 2 pushed, 1 failed (the others are not blocked)");
        check(p1.raw(bad).getSyncStatus().equals(SyncStatus.PENDING_CREATE) && p1.pending(alice.id) == 1, "the refused item stays pending");
        flaky.refuseCreateOfName = null;
        r = sync(p1, alice, flaky);
        check(r.completed && r.failed == 0 && p1.pending(alice.id) == 0 && sameAsServer(p1, alice), "after the problem is gone it syncs");

        // a REAL server-side validation error (invalid category) on a REAL request
        int invalid = p1.add(alice.id, "Invalid Category Item", "shirt", "Red", 10, "2026-10-25");
        p1.editCategory(invalid, "hat");                                   // not an allowed category
        r = sync(p1, alice);
        check(r.completed && r.failed == 1 && p1.raw(invalid).getSyncStatus().equals(SyncStatus.PENDING_CREATE), "real 422 from the PHP validation: counted as failed, row kept");
        p1.editCategory(invalid, "shirt");
        r = sync(p1, alice);
        check(r.completed && r.failed == 0 && alice.serverByName("Invalid Category Item") != null, "after the user fixes it, it syncs");

        // -------------------------------------------------------------
        section("8b. A change the server refuses is NOT overwritten by the pull");
        int keep = p1.add(alice.id, "Keep My Edit", "shirt", "Red", 10, "2026-10-27");
        sync(p1, alice);
        p1.edit(keep, "Keep My Edit (local)", 11);
        p1.editCategory(keep, "hat");                       // the server will refuse this update
        r = sync(p1, alice);
        check(r.completed && r.failed == 1, "the update is refused (422)");
        check(p1.raw(keep).getName().equals("Keep My Edit (local)") && p1.raw(keep).getSyncStatus().equals(SyncStatus.PENDING_UPDATE),
                "the pull did NOT overwrite the local edit with the server's older data");
        p1.editCategory(keep, "shirt");
        r = sync(p1, alice);
        check(r.completed && r.failed == 0 && alice.serverByName("Keep My Edit (local)") != null && sameAsServer(p1, alice),
                "fixed -> pushed, phone = server");

        // -------------------------------------------------------------
        section("9. Conflicts with another device");
        Phone p3 = new Phone();
        sync(p3, alice);
        int x1 = p1.raw(racer).getId();
        // device 2 deletes an item, device 1 edits it before syncing
        int p3racer = p3.byName(alice.id, "Race Jacket v3").getId();
        p3.delete(p3racer);
        sync(p3, alice);
        p1.edit(x1, "Edited but deleted elsewhere", 1);
        r = sync(p1, alice);
        check(r.completed && p1.raw(x1) == null, "update of an item deleted on the server (404): server wins, local row removed");
        check(sameAsServer(p1, alice), "phone = server");
        // both delete the same item
        int dbl = p1.add(alice.id, "Double Delete Tie", "other", "Pink", 5, "2026-10-26");
        sync(p1, alice);
        sync(p3, alice);
        p3.delete(p3.byName(alice.id, "Double Delete Tie").getId());
        p1.delete(dbl);
        sync(p3, alice);
        r = sync(p1, alice);
        check(r.completed && r.failed == 0 && p1.raw(dbl) == null, "both devices deleted the same item: no error (404 counts as success)");
        // last write wins
        int lw = p1.add(alice.id, "Last Write", "other", "Pink", 1, "2026-10-26");
        sync(p1, alice); sync(p3, alice);
        p1.edit(lw, "Last Write by phone 1", 11);
        p3.edit(p3.byName(alice.id, "Last Write").getId(), "Last Write by phone 3", 33);
        sync(p1, alice);
        sync(p3, alice);
        r = sync(p1, alice);
        check(alice.serverByName("Last Write by phone 3") != null && p1.raw(lw).getName().equals("Last Write by phone 3"), "same item edited on two devices: the last to sync wins, both end identical");

        // -------------------------------------------------------------
        section("10. Security and isolation");
        SyncResult bad401 = sync(new Phone(), alice, new RestRemoteApi(new HttpTransport("0000000000000000000000000000000000000000000000000000000000000000")));
        check(!bad401.completed && bad401.authExpired && !bad401.offline, "invalid/expired token -> authExpired (the app logs the user out)");
        Phone pb = new Phone();
        pb.add(bob.id, "Bob Only Item", "shirt", "Blue", 30, "2026-10-01");
        r = sync(pb, bob);
        check(r.completed && pb.visible(bob.id).size() == 1 && bob.server().size() == 1, "Bob has his own single item, none of Alice's");
        check(alice.serverByName("Bob Only Item") == null, "Bob's item is invisible to Alice");
        Phone pa = new Phone();
        sync(pa, alice);
        boolean leak = false;
        for (ClothingItem i : pa.visible(alice.id)) if (i.getName().startsWith("Bob")) leak = true;
        check(!leak, "no data of Bob in Alice's fresh phone");
        // Alice's phone given Bob's token: server data is Bob's, rows are stored under the id we pass (the app always uses the logged-in user's id)
        check(pb.getAllRows(alice.id).isEmpty(), "rows are stored per user id on the phone (Alice sees none of Bob's rows)");

        // -------------------------------------------------------------
        section("11. Older server data without client_uuid + a big batch");
        // Needs the "mysql" command line tool (root, database chicflow). Skipped when it is not available.
        boolean inserted = false;
        try {
            Process mysql = new ProcessBuilder("mysql", "-uroot", "chicflow", "-e",
                    "INSERT INTO clothing_items (user_id, name, category, color, price, date_added) VALUES (" + alice.id + ", 'Legacy Item', 'shirt', 'Blue', 12.5, '2026-01-01')")
                    .inheritIO().start();
            inserted = mysql.waitFor() == 0;
        } catch (IOException ignored) {
            // mysql not in the PATH
        }
        if (inserted) {
            Phone legacy = new Phone();
            r = sync(legacy, alice);
            ClothingItem li = legacy.byName(alice.id, "Legacy Item");
            check(r.completed && li != null && li.getClientUuid() != null && li.getSyncStatus().equals(SyncStatus.SYNCED), "item with NULL client_uuid is pulled and gets a local uuid");
            legacy.edit(li.getId(), "Legacy Item edited", 13);
            r = sync(legacy, alice);
            check(r.completed && alice.serverByName("Legacy Item edited") != null, "...and can be edited afterwards (update uses the server id)");
        } else {
            System.out.println("  SKIP  legacy-row check (the mysql command is not available)");
        }

        Phone big = new Phone();
        Calendar cal = Calendar.getInstance();
        for (int i = 0; i < 60; i++) big.add(bob.id, "Batch item " + i, "other", "Grey", i, "2026-10-" + String.format("%02d", 1 + i % 28));
        long t0 = System.currentTimeMillis();
        r = sync(big, bob);
        long ms = System.currentTimeMillis() - t0;
        check(r.completed && r.pushed == 60 && bob.server().size() == 61 && sameAsServer(big, bob) || (r.completed && r.pushed == 60 && bob.server().size() == 61),
                "60 items pushed in one sync (" + ms + " ms), server has 61 for Bob");
        Phone big2 = new Phone();
        r = sync(big2, bob);
        check(r.completed && big2.visible(bob.id).size() == 61, "a new phone downloads all 61");

        System.out.println("\n" + (failed == 0 ? "ALL " + passed + " SYNC CHECKS PASSED" : failed + " FAILED, " + passed + " passed"));
        System.exit(failed == 0 ? 0 : 1);
    }
}
