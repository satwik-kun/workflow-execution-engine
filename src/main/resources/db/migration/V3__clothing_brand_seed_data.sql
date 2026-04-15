CREATE TABLE IF NOT EXISTS cb_suppliers (
    supplier_id INTEGER PRIMARY KEY,
    supplier_name VARCHAR(255) NOT NULL,
    city VARCHAR(120) NOT NULL,
    lead_time_days INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS cb_products (
    sku VARCHAR(40) PRIMARY KEY,
    product_name VARCHAR(255) NOT NULL,
    category VARCHAR(120) NOT NULL,
    size_range VARCHAR(50) NOT NULL,
    colorway VARCHAR(120) NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    supplier_id INTEGER NOT NULL,
    CONSTRAINT fk_cb_products_supplier FOREIGN KEY (supplier_id) REFERENCES cb_suppliers(supplier_id)
);

CREATE TABLE IF NOT EXISTS cb_inventory (
    sku VARCHAR(40) PRIMARY KEY,
    on_hand_qty INTEGER NOT NULL,
    reserved_qty INTEGER NOT NULL,
    reorder_level INTEGER NOT NULL,
    CONSTRAINT fk_cb_inventory_product FOREIGN KEY (sku) REFERENCES cb_products(sku)
);

CREATE TABLE IF NOT EXISTS cb_purchase_orders (
    po_id VARCHAR(40) PRIMARY KEY,
    sku VARCHAR(40) NOT NULL,
    order_qty INTEGER NOT NULL,
    channel VARCHAR(80) NOT NULL,
    order_status VARCHAR(80) NOT NULL,
    planned_ship_date DATE NOT NULL,
    CONSTRAINT fk_cb_purchase_orders_product FOREIGN KEY (sku) REFERENCES cb_products(sku)
);

MERGE INTO cb_suppliers (supplier_id, supplier_name, city, lead_time_days) KEY (supplier_id)
VALUES
    (101, 'BlueLoom Textiles', 'Tiruppur', 7),
    (102, 'NorthStitch Manufacturing', 'Bengaluru', 10),
    (103, 'UrbanTrim Accessories', 'Delhi', 5);

MERGE INTO cb_products (sku, product_name, category, size_range, colorway, unit_price, supplier_id) KEY (sku)
VALUES
    ('UT-SS26-001', 'Summer Drop Hoodie', 'Outerwear', 'S-XL', 'Sand Beige', 1799.00, 101),
    ('UT-DNM-014', 'Weekend Denim Jacket', 'Jackets', 'M-XXL', 'Indigo Wash', 2599.00, 102),
    ('UT-ATH-022', 'Athleisure Co-ord Set', 'Athleisure', 'S-L', 'Olive Green', 2199.00, 103);

MERGE INTO cb_inventory (sku, on_hand_qty, reserved_qty, reorder_level) KEY (sku)
VALUES
    ('UT-SS26-001', 140, 32, 60),
    ('UT-DNM-014', 90, 25, 40),
    ('UT-ATH-022', 120, 18, 50);

MERGE INTO cb_purchase_orders (po_id, sku, order_qty, channel, order_status, planned_ship_date) KEY (po_id)
VALUES
    ('PO-UT-24001', 'UT-SS26-001', 24, 'Online', 'PENDING_APPROVAL', DATE '2026-04-20'),
    ('PO-UT-24002', 'UT-DNM-014', 18, 'Retail', 'IN_PRODUCTION', DATE '2026-04-22'),
    ('PO-UT-24003', 'UT-ATH-022', 30, 'Marketplace', 'READY_FOR_DISPATCH', DATE '2026-04-19');