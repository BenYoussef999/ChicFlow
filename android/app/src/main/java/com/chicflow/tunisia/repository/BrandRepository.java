package com.chicflow.tunisia.repository;

import com.chicflow.tunisia.models.Brand;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The Local Style catalog. It starts EMPTY on purpose: brands, prices and links must be real,
 * so an administrator adds them (later this table is filled on the server).
 */
public class BrandRepository {

    private static BrandRepository instance;

    private final List<Brand> brands = new ArrayList<>();
    private int nextId = 1;

    public static synchronized BrandRepository getInstance() {
        if (instance == null) {
            instance = new BrandRepository();
        }
        return instance;
    }

    private BrandRepository() { }

    /** All brands, A to Z. */
    public List<Brand> getAll() {
        List<Brand> result = new ArrayList<>(brands);
        Collections.sort(result, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        return result;
    }

    public Brand add(Brand brand) {
        brand.setId(nextId++);
        brands.add(brand);
        return brand;
    }

    public void delete(int id) {
        for (int i = 0; i < brands.size(); i++) {
            if (brands.get(i).getId() == id) {
                brands.remove(i);
                return;
            }
        }
    }
}
