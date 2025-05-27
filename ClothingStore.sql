USE master;
GO

-- Drop the database if it exists
IF EXISTS (SELECT name FROM sys.databases WHERE name = 'ClothingACVShop')
BEGIN
    DROP DATABASE ClothingACVShop;
END
GO

-- Create the database
CREATE DATABASE ClothingACVShop;
GO

-- Set the Vietnamese collation
ALTER DATABASE ClothingACVShop
COLLATE Vietnamese_CI_AS;
GO

-- Switch to the new database
USE ClothingACVShop;
GO

-- Table Users
CREATE TABLE Users (
                       id INT IDENTITY(1,1) PRIMARY KEY,
                       username NVARCHAR(50) UNIQUE NOT NULL,
                       password NVARCHAR(255) NULL,
                       full_name NVARCHAR(100),
                       email NVARCHAR(100) UNIQUE,
                       phone NVARCHAR(15),
                       address NVARCHAR(255),
                       role NVARCHAR(20) CHECK (role IN ('ADMIN', 'EMPLOYEE', 'CUSTOMER')),
                       date_of_birth DATE,
                       id_card NVARCHAR(20),
                       gender NVARCHAR(50),
                       province NVARCHAR(100),
                       district NVARCHAR(100),
                       ward NVARCHAR(100),
                       address_detail NVARCHAR(255),
                       created_at DATETIME DEFAULT GETDATE(),
                       is_deleted BIT DEFAULT 0,
                       reset_otp VARCHAR(6) NULL,
                       otp_expiry DATETIME NULL
);
GO

-- Table Categories
CREATE TABLE Categories (
                            id INT IDENTITY(1,1) PRIMARY KEY,
                            name NVARCHAR(100) NOT NULL UNIQUE,
                            description NVARCHAR(255),
                            created_at DATETIME DEFAULT GETDATE()
);
GO

-- Table Products
CREATE TABLE Products (
                          id INT IDENTITY(1,1) PRIMARY KEY,
                          name NVARCHAR(255) NOT NULL,
                          description NVARCHAR(1000),
                          category_id INT,
                          image_url VARCHAR(MAX),
    created_at DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (category_id) REFERENCES Categories(id) ON DELETE SET NULL
);
GO

-- Tables Origin, Color, Size, Materials, Style
CREATE TABLE Origins (
                         id INT IDENTITY(1,1) PRIMARY KEY,
                         name NVARCHAR(100) UNIQUE NOT NULL
);
GO

CREATE TABLE Colors (
                        id INT IDENTITY(1,1) PRIMARY KEY,
                        name NVARCHAR(50) UNIQUE NOT NULL
);
GO

CREATE TABLE Sizes (
                       id INT IDENTITY(1,1) PRIMARY KEY,
                       name NVARCHAR(20) UNIQUE NOT NULL
);
GO

CREATE TABLE Materials (
                           id INT IDENTITY(1,1) PRIMARY KEY,
                           name NVARCHAR(100) UNIQUE NOT NULL
);
GO

CREATE TABLE Styles (
                        id INT IDENTITY(1,1) PRIMARY KEY,
                        name NVARCHAR(100) UNIQUE NOT NULL
);
GO

-- Table Product_Details
CREATE TABLE Product_Details (
                                 id INT IDENTITY(1,1) PRIMARY KEY,
                                 product_id INT NOT NULL,
                                 origin_id INT,
                                 color_id INT,
                                 size_id INT,
                                 material_id INT,
                                 style_id INT,
                                 price DECIMAL(10,2) NOT NULL,
                                 stock_quantity INT DEFAULT 0,
                                 created_at DATETIME DEFAULT GETDATE(),
                                 FOREIGN KEY (product_id) REFERENCES Products(id) ON DELETE CASCADE,
                                 FOREIGN KEY (origin_id) REFERENCES Origins(id) ON DELETE SET NULL,
                                 FOREIGN KEY (color_id) REFERENCES Colors(id) ON DELETE SET NULL,
                                 FOREIGN KEY (size_id) REFERENCES Sizes(id) ON DELETE SET NULL,
                                 FOREIGN KEY (material_id) REFERENCES Materials(id) ON DELETE SET NULL,
                                 FOREIGN KEY (style_id) REFERENCES Styles(id) ON DELETE SET NULL
);
GO

-- Add unique constraint to Product_Details to prevent duplicates
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'unique_product_color_size' AND object_id = OBJECT_ID('Product_Details'))
BEGIN
ALTER TABLE Product_Details
    ADD CONSTRAINT unique_product_color_size UNIQUE (product_id, color_id, size_id);
END
GO

-- Table Product_Images
CREATE TABLE Product_Images (
                                id INT IDENTITY(1,1) PRIMARY KEY,
                                product_detail_id INT,
                                image_url VARCHAR(MAX),
    uploaded_at DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (product_detail_id) REFERENCES Product_Details(id) ON DELETE CASCADE
);
GO

-- Table Employees
CREATE TABLE Employees (
                           id INT IDENTITY(1,1) PRIMARY KEY,
                           user_id INT NOT NULL,
                           employee_code NVARCHAR(20) UNIQUE,
                           hire_date DATE,
                           department NVARCHAR(100),
                           created_at DATETIME DEFAULT GETDATE(),
                           is_deleted BIT DEFAULT 0,
                           status BIT DEFAULT 1, -- 1 = Active, 0 = Inactive
                           FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE
);
GO

-- Table Orders
CREATE TABLE Orders (
                        id INT IDENTITY(1,1) PRIMARY KEY,
                        user_id INT,
                        total_price DECIMAL(10,2) NOT NULL,
                        status NVARCHAR(20) DEFAULT 'Pending',
                        created_at DATETIME DEFAULT GETDATE(),
                        code NVARCHAR(20),
                        shipping_fee DECIMAL(10,2) DEFAULT 0,
                        delivery_method NVARCHAR(50),
                        payment_method NVARCHAR(50),
                        FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE
);
GO

-- Table Cart
CREATE TABLE Cart (
                      id INT IDENTITY(1,1) PRIMARY KEY,
                      user_id INT NOT NULL,
                      product_id INT NOT NULL,
                      quantity INT NOT NULL DEFAULT 1,
                      created_at DATETIME DEFAULT GETDATE(),
                      FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE,
                      FOREIGN KEY (product_id) REFERENCES Products(id) ON DELETE CASCADE
);
GO

-- Table Order_Details
CREATE TABLE Order_Details (
                               id INT IDENTITY(1,1) PRIMARY KEY,
                               order_id INT NOT NULL,
                               product_detail_id INT NOT NULL,
                               quantity INT NOT NULL,
                               price DECIMAL(10,2) NOT NULL,
                               product_name NVARCHAR(255),
                               FOREIGN KEY (order_id) REFERENCES Orders(id) ON DELETE CASCADE,
                               FOREIGN KEY (product_detail_id) REFERENCES Product_Details(id) ON DELETE CASCADE
);
GO

-- Table Checkout
CREATE TABLE Checkout (
                          id INT IDENTITY(1,1) PRIMARY KEY,
                          user_id INT NOT NULL,
                          total_price DECIMAL(10,2) NOT NULL,
                          status NVARCHAR(20) NOT NULL DEFAULT 'Pending'
);
GO

-- Table Reviews
CREATE TABLE Reviews (
                         id INT IDENTITY(1,1) PRIMARY KEY,
                         user_id INT NOT NULL,
                         product_id INT NOT NULL,
                         rating INT CHECK (rating BETWEEN 1 AND 5),
                         comment NVARCHAR(1000),
                         created_at DATETIME DEFAULT GETDATE(),
                         FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE,
                         FOREIGN KEY (product_id) REFERENCES Products(id) ON DELETE CASCADE
);
GO

-- Table Notifications
CREATE TABLE Notifications (
                               id INT IDENTITY(1,1) PRIMARY KEY,
                               user_id INT NOT NULL,
                               message NVARCHAR(255) NOT NULL,
                               is_read BIT DEFAULT 0,
                               created_at DATETIME DEFAULT GETDATE(),
                               FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE
);
GO

-- Table Promotions
CREATE TABLE Promotions (
                            id INT IDENTITY(1,1) PRIMARY KEY,
                            name NVARCHAR(100) NOT NULL,
                            discount_percentage DECIMAL(5,2) NOT NULL,
                            start_date DATE NOT NULL,
                            end_date DATE NOT NULL,
                            created_at DATETIME DEFAULT GETDATE()
);
GO

-- Table DiscountVoucher
CREATE TABLE discount_voucher (
                                  id INT IDENTITY(1,1) PRIMARY KEY,
                                  code NVARCHAR(50) UNIQUE NOT NULL,
                                  name NVARCHAR(100) NOT NULL,
                                  type NVARCHAR(10) CHECK (type IN ('PERCENT', 'CASH')) NOT NULL,
                                  discount_value DECIMAL(10,2) NOT NULL,
                                  max_discount_value DECIMAL(10,2),
                                  min_order_value DECIMAL(10,2),
                                  quantity INT DEFAULT 0,
                                  usage_count INT DEFAULT 0,
                                  is_public BIT DEFAULT 1,
                                  start_date DATETIME NOT NULL,
                                  end_date DATETIME NOT NULL,
                                  created_at DATETIME DEFAULT GETDATE(),
                                  is_deleted BIT DEFAULT 0
);
GO

-- Table DiscountCampaigns
CREATE TABLE discount_campaigns (
                                    id INT IDENTITY(1,1) PRIMARY KEY,
                                    code NVARCHAR(50) UNIQUE NOT NULL,
                                    name NVARCHAR(100) NOT NULL,
                                    discount_percent DECIMAL(5,2) NOT NULL,
                                    quantity INT DEFAULT 0,
                                    start_date DATETIME NOT NULL,
                                    end_date DATETIME NOT NULL,
                                    created_at DATETIME DEFAULT GETDATE(),
                                    is_deleted BIT DEFAULT 0
);
GO

-- Table discount_campaign_products
CREATE TABLE discount_campaign_products (
                                            id INT IDENTITY(1,1) PRIMARY KEY,
                                            campaign_id INT NOT NULL,
                                            product_id INT NOT NULL,
                                            FOREIGN KEY (campaign_id) REFERENCES discount_campaigns(id) ON DELETE CASCADE,
                                            FOREIGN KEY (product_id) REFERENCES Products(id) ON DELETE CASCADE
);
GO

-- Table user_discount_voucher
CREATE TABLE user_discount_voucher (
                                       id INT IDENTITY(1,1) PRIMARY KEY,
                                       user_id INT NOT NULL,
                                       voucher_id INT NOT NULL,
                                       created_at DATETIME DEFAULT GETDATE(),
                                       FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE,
                                       FOREIGN KEY (voucher_id) REFERENCES discount_voucher(id) ON DELETE CASCADE
);
GO

-- Table discount_campaign_product_details
CREATE TABLE discount_campaign_product_details (
                                                   id INT IDENTITY(1,1) PRIMARY KEY,
                                                   campaign_id INT NOT NULL,
                                                   product_detail_id INT NOT NULL,
                                                   FOREIGN KEY (campaign_id) REFERENCES discount_campaigns(id) ON DELETE CASCADE,
                                                   FOREIGN KEY (product_detail_id) REFERENCES Product_Details(id) ON DELETE CASCADE
);
GO

-- Insert sample data
INSERT INTO Users (username, password, full_name, email, phone, address, role, date_of_birth, id_card, gender, province, district, ward, address_detail) VALUES
    ('admin', 'admin123', N'Admin User', 'admin@example.com', '0123456789', N'Admin Address', 'ADMIN', NULL, '111111111111', NULL, NULL, NULL, NULL, NULL),
    ('staff1', 'staffpass', N'Staff Member', 'staff@example.com', '0987654321', N'Staff Address', 'EMPLOYEE', NULL, '222222222222', NULL, NULL, NULL, NULL, NULL),
    ('customer1', 'customerpass', N'Customer One', 'customer@example.com', '0912345678', N'Customer Address', 'CUSTOMER', '1995-05-15', '123456789012', N'Nam', N'Hà Nội', N'Ba Đình', N'Phúc Xá', N'123 Đường Láng'),
    ('hoanganh01', '123456', N'Hoàng Anh', 'hoanganh@gmail.com', '0911111111', N'Đà Nẵng', 'CUSTOMER', '1998-03-10', '987654321012', N'Nữ', N'Đà Nẵng', N'Hải Châu', N'Thanh Bình', N'45 Lê Lợi'),
    ('thutrang02', '123456', N'Thùy Trang', 'namhaihoang3103@gmail.com', '0922222222', N'Hải Phòng', 'CUSTOMER', '1997-07-22', '456789123012', N'Nữ', N'Hải Phòng', N'Hồng Bàng', N'Hạ Lý', N'78 Nguyễn Trãi'),
    ('guest', 'guest123', N'Khách lẻ', 'guest@example.com', '0999999999', N'Chưa xác định', 'CUSTOMER', NULL, NULL, NULL, NULL, NULL, NULL, NULL);
GO

-- Insert into Categories
INSERT INTO Categories (name, description) VALUES
    (N'Áo Nam', N'Các loại áo nam thời trang, trẻ trung và lịch lãm'),
    (N'Áo Nữ', N'Áo nữ đa phong cách, từ năng động đến nữ tính'),
    (N'Quần Nam', N'Quần nam jeans, kaki, jogger chất lượng cao'),
    (N'Quần Nữ', N'Quần nữ legging, short, quần dài thời thượng'),
    (N'Phụ Kiện', N'Phụ kiện thời trang như mũ, khăn, thắt lưng');
GO

-- Insert into Products
INSERT INTO Products (name, description, category_id, image_url) VALUES
    (N'Áo Thun Nam Đen Basic', N'Áo thun nam cotton mềm mại, phong cách tối giản', 1, 'https://example.com/images/ao-thun-den-basic.jpg'),
    (N'Áo Sơ Mi Nam Trắng Công Sở', N'Áo sơ mi nam form dáng lịch lãm, phù hợp công sở', 1, 'https://example.com/images/ao-so-mi-trang.jpg'),
    (N'Áo Polo Nam Xanh Navy', N'Áo polo nam thoáng mát, năng động', 1, 'https://example.com/images/ao-polo-xanh-navy.jpg'),
    (N'Áo Khoác Nam Hoodie Xám', N'Áo hoodie nam ấm áp, phong cách trẻ trung', 1, 'https://example.com/images/ao-hoodie-xam.jpg'),
    (N'Áo Thun Nam In Họa Tiết', N'Áo thun nam với họa tiết độc đáo', 1, 'https://example.com/images/ao-thun-hoa-tiet.jpg'),
    (N'Áo Sơ Mi Nam Caro Đỏ', N'Áo sơise mi nam sơ mi nam caro đỏ', 1, 'https://example.com/images/ao-so-mi-caro-do.jpg'),
    (N'Áo Polo Nam Đen Thể Thao', N'Áo polo nam phong cách thể thao, co giãn tốt', 1, 'https://example.com/images/ao-polo-den-the-thao.jpg'),
    (N'Áo Thun Nam Dài Tay Xanh', N'Áo thun nam dài tay, phù hợp mùa thu', 1, 'https://example.com/images/ao-thun-dai-tay-xanh.jpg'),
    (N'Áo Vest Nam Xanh Đậm', N'Áo vest nam lịch lãm, phù hợp sự kiện', 1, 'https://example.com/images/ao-vest-xanh-dam.jpg'),
    (N'Áo Len Nam Nâu', N'Áo len nam ấm áp, phong cách cổ điển', 1, 'https://example.com/images/ao-len-nau.jpg');
GO

-- Insert into Origins
INSERT INTO Origins (name) VALUES
    (N'Vietnam'), (N'USA'), (N'China'), (N'Thailand'), (N'Korea'), (N'Japan'), (N'Germany'), (N'France');
GO

-- Insert into Colors
INSERT INTO Colors (name) VALUES
    (N'Black'), (N'White'), (N'Red'), (N'Blue'), (N'Green'), (N'Yellow'), (N'Gray'), (N'Pink');
GO

-- Insert into Sizes
INSERT INTO Sizes (name) VALUES
    (N'S'), (N'M'), (N'L'), (N'XL'), (N'XXL');
GO

-- Insert into Materials
INSERT INTO Materials (name) VALUES
    (N'Leather'), (N'Mesh'), (N'Synthetic'), (N'Cotton'), (N'Denim'), (N'Linen'), (N'Wool'), (N'Silk');
GO

-- Insert into Styles
INSERT INTO Styles (name) VALUES
    (N'Sporty'), (N'Casual'), (N'Classic'), (N'Formal'), (N'Minimal'), (N'Streetwear'), (N'Vintage'), (N'Boho');
GO

-- Insert into Product_Details
INSERT INTO Product_Details (product_id, origin_id, color_id, size_id, material_id, style_id, price, stock_quantity) VALUES
    -- 1. Áo Thun Nam Đen Basic (product_id=1)
    (1, 1, 1, 2, 4, 2, 199000, 50), -- Black, M, Cotton, Casual
    (1, 1, 1, 3, 4, 2, 199000, 40), -- Black, L, Cotton, Casual
    (1, 1, 1, 4, 4, 2, 199000, 30), -- Black, XL, Cotton, Casual
    (1, 1, 2, 2, 4, 2, 199000, 20), -- White, M, Cotton, Casual
    -- 2. Áo Sơ Mi Nam Trắng Công Sở (product_id=2)
    (2, 1, 2, 2, 4, 3, 299000, 30), -- White, M, Cotton, Classic
    (2, 1, 2, 3, 4, 3, 299000, 25), -- White, L, Cotton, Classic
    (2, 1, 2, 4, 4, 3, 299000, 20), -- White, XL, Cotton, Classic
    (2, 1, 7, 3, 4, 3, 299000, 15), -- Gray, L, Cotton, Classic
    -- 3. Áo Polo Nam Xanh Navy (product_id=3)
    (3, 1, 4, 2, 4, 2, 249000, 35), -- Blue, M, Cotton, Casual
    (3, 1, 4, 3, 4, 2, 249000, 30), -- Blue, L, Cotton, Casual
    (3, 1, 4, 4, 4, 2, 249000, 25), -- Blue, XL, Cotton, Casual
    -- 4. Áo Khoác Nam Hoodie Xám (product_id=4)
    (4, 2, 7, 2, 4, 2, 399000, 25), -- Gray, M, Cotton, Casual
    (4, 2, 7, 3, 4, 2, 399000, 20), -- Gray, L, Cotton, Casual
    (4, 2, 7, 4, 4, 2, 399000, 15), -- Gray, XL, Cotton, Casual
    (4, 2, 1, 3, 4, 2, 399000, 10), -- Black, L, Cotton, Casual
    -- 5. Áo Thun Nam In Họa Tiết (product_id=5)
    (5, 1, 1, 2, 4, 2, 229000, 40), -- Black, M, Cotton, Casual
    (5, 1, 1, 3, 4, 2, 229000, 35), -- Black, L, Cotton, Casual
    (5, 1, 1, 4, 4, 2, 229000, 30), -- Black, XL, Cotton, Casual
    -- 6. Áo Sơ Mi Nam Caro Đỏ (product_id=6)
    (6, 1, 3, 2, 4, 2, 279000, 30), -- Red, M, Cotton, Casual
    (6, 1, 3, 3, 4, 2, 279000, 25), -- Red, L, Cotton, Casual
    (6, 1, 3, 4, 4, 2, 279000, 20), -- Red, XL, Cotton, Casual
    -- 7. Áo Polo Nam Đen Thể Thao (product_id=7)
    (7, 1, 1, 2, 4, 1, 259000, 35), -- Black, M, Cotton, Sporty
    (7, 1, 1, 3, 4, 1, 259000, 30), -- Black, L, Cotton, Sporty
    (7, 1, 1, 4, 4, 1, 259000, 25), -- Black, XL, Cotton, Sporty
    (7, 1, 4, 3, 4, 1, 259000, 20), -- Blue, L, Cotton, Sporty
    -- 8. Áo Thun Nam Dài Tay Xanh (product_id=8)
    (8, 1, 5, 2, 4, 2, 239000, 30), -- Green, M, Cotton, Casual
    (8, 1, 5, 3, 4, 2, 239000, 25), -- Green, L, Cotton, Casual
    (8, 1, 5, 4, 4, 2, 239000, 20), -- Green, XL, Cotton, Casual
    -- 9. Áo Vest Nam Xanh Đậm (product_id=9)
    (9, 2, 4, 2, 7, 4, 599000, 15), -- Blue, M, Wool, Formal
    (9, 2, 4, 3, 7, 4, 599000, 12), -- Blue, L, Wool, Formal
    (9, 2, 4, 4, 7, 4, 599000, 10), -- Blue, XL, Wool, Formal
    -- 10. Áo Len Nam Nâu (product_id=10)
    (10, 1, 6, 2, 7, 3, 349000, 20), -- Yellow (brown), M, Wool, Classic
    (10, 1, 6, 3, 7, 3, 349000, 18), -- Yellow (brown), L, Wool, Classic
    (10, 1, 6, 4, 7, 3, 349000, 15); -- Yellow (brown), XL, Wool, Classic
GO

-- Insert into Employees
INSERT INTO Employees (user_id, employee_code, hire_date, department, status) VALUES
    (2, N'NV001', '2023-01-15', N'Kinh doanh', 1);
GO

-- Insert into Orders
INSERT INTO Orders (code, user_id, total_price, shipping_fee, delivery_method, payment_method, status) VALUES
    (N'HD001', 3, 428000, 30000, N'Giao hàng', N'Chuyển khoản', N'Pending'),
    (N'HD002', 4, 249000, 20000, N'Giao hàng', N'Tiền mặt', N'Completed'),
    (N'HD003', 5, 598000, 30000, N'Giao hàng', N'Chuyển khoản', N'Pending'),
    (N'HD004', 3, 299000, 0, N'Tại quầy', N'Tiền mặt', N'Completed');
GO

-- Insert into Order_Details
INSERT INTO Order_Details (order_id, product_detail_id, quantity, price, product_name) VALUES
    (1, 1, 2, 199000, N'Áo Thun Nam Đen Basic'), -- 2 Black M Áo Thun
    (2, 9, 1, 249000, N'Áo Polo Nam Xanh Navy'), -- 1 Blue M Áo Polo
    (3, 31, 1, 599000, N'Áo Vest Nam Xanh Đậm'), -- 1 Blue M Áo Vest
    (4, 5, 1, 299000, N'Áo Sơ Mi Nam Trắng Công Sở'); -- 1 White M Áo Sơ Mi
GO

-- Insert into Cart
INSERT INTO Cart (user_id, product_id, quantity) VALUES
    (3, 1, 2), -- Customer1 adds 2 Áo Thun Nam Đen Basic
    (4, 3, 1), -- Customer2 adds 1 Áo Polo Nam Xanh Navy
    (5, 4, 1), -- Customer3 adds 1 Áo Khoác Nam Hoodie Xám
    (5, 2, 2); -- Customer3 adds 2 Áo Sơ Mi Nam Trắng Công Sở
GO

-- Insert into Reviews
INSERT INTO Reviews (user_id, product_id, rating, comment) VALUES
    (3, 1, 5, N'Áo đẹp, chất liệu tốt!'),
    (4, 1, 4, N'Sản phẩm tốt, đúng mô tả'),
    (5, 2, 5, N'Mẫu mã đẹp, giao hàng nhanh');
GO

-- Insert into Notifications
INSERT INTO Notifications (user_id, message, is_read) VALUES
    (3, N'Đơn hàng của bạn đã được xác nhận!', 0),
    (4, N'Đơn hàng của bạn đã được giao', 1),
    (5, N'Bạn đã nhận được ưu đãi mới', 0);
GO

-- Insert into Promotions
INSERT INTO Promotions (name, discount_percentage, start_date, end_date) VALUES
    (N'Sale Tốt 2025', 20.00, '2025-01-10', '2025-02-10'),
    (N'Flash Sale 11.11', 30.00, '2025-11-11', '2025-11-11'),
    (N'Hot Deal 5.5', 25.00, '2025-05-05', '2025-05-10'),
    (N'Mid Year Sale', 15.00, '2025-06-01', '2025-06-15'),
    (N'Back to School', 10.00, '2025-08-01', '2025-08-31'),
    (N'Black Friday', 50.00, '2025-11-25', '2025-11-29'),
    (N'Xmas Deal', 30.00, '2025-12-20', '2025-12-26');
GO

-- Insert into discount_voucher
INSERT INTO discount_voucher (code, name, type, discount_value, max_discount_value, min_order_value, quantity, usage_count, is_public, start_date, end_date) VALUES
    (N'GGTHANG6', N'Giảm giá tháng 5', 'PERCENT', 15.00, 150000, 1000000, 12, 0, 1, '2024-04-29 19:40:00', '2026-05-12 19:39:00'),
    (N'GGMUNGLE', N'Giảm giá mừng lễ', 'PERCENT', 10.00, 200000, 100000, 96, 0, 1, '2024-04-29 19:34:00', '2024-05-12 19:34:00');
GO

-- Insert into discount_campaigns
INSERT INTO discount_campaigns (code, name, discount_percent, quantity, start_date, end_date) VALUES
    (N'DGG219CA56', N'Giảm giá mừng lễ', 5.00, 21, '2024-04-29 19:40:00', '2024-05-12 19:39:00');
GO

-- Insert into user_discount_voucher
INSERT INTO user_discount_voucher (user_id, voucher_id) VALUES
    (3, 1),
    (5, 2);
GO

-- Query to check vouchers for customers
SELECT
    u.id AS user_id,
    u.full_name,
    u.email,
    dv.code AS voucher_code,
    dv.name AS voucher_name,
    dv.type,
    dv.discount_value,
    dv.is_public
FROM Users u
         LEFT JOIN user_discount_voucher udv ON u.id = udv.user_id
         LEFT JOIN discount_voucher dv ON (dv.id = udv.voucher_id OR dv.is_public = 1)
WHERE u.role = 'CUSTOMER' AND u.is_deleted = 0 AND (dv.is_deleted = 0 OR dv.id IS NULL)
ORDER BY u.id;
GO

-- Query to check personal vouchers assigned to users
SELECT
    u.id AS user_id,
    u.full_name,
    u.email,
    dv.code AS voucher_code,
    dv.name AS voucher_name,
    dv.type,
    dv.discount_value,
    'CÁ NHÂN' AS voucher_type
FROM Users u
         JOIN user_discount_voucher udv ON u.id = udv.user_id
         JOIN discount_voucher dv ON dv.id = udv.voucher_id
WHERE u.role = 'CUSTOMER' AND u.is_deleted = 0 AND dv.is_deleted = 0;
GO

-- Query to check Product_Details
SELECT * FROM Product_Details;
GO

-- Query to check Products
SELECT * FROM Products;
GO

-- Query to check Users table columns
SELECT COLUMN_NAME, DATA_TYPE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'Users';
GO

-- Check for duplicates in Product_Details
SELECT product_id, color_id, size_id, COUNT(*) as count
FROM Product_Details
GROUP BY product_id, color_id, size_id
HAVING COUNT(*) > 1;
GO

-- Remove duplicates if any
DELETE FROM Product_Details
WHERE id IN (
    SELECT id FROM (
                       SELECT id,
                              ROW_NUMBER() OVER (PARTITION BY product_id, color_id, size_id ORDER BY id) AS row_num
                       FROM Product_Details
                   ) t
    WHERE row_num > 1
);
GO

-- Check discount campaigns
SELECT * FROM discount_campaigns;
SELECT * FROM discount_campaigns WHERE is_deleted = 0;
GO

-- Check Product_Details for a specific product
SELECT
    pd.id AS product_detail_id,
    p.name AS product_name,
    c.name AS color_name,
    s.name AS size_name,
    pd.price,
    pd.stock_quantity
FROM Product_Details pd
         JOIN Products p ON pd.product_id = p.id
         LEFT JOIN Colors c ON pd.color_id = c.id
         LEFT JOIN Sizes s ON pd.size_id = s.id
WHERE pd.product_id = 1;
GO

-- Check Product_Details for product_id = 1
SELECT * FROM Product_Details WHERE product_id = 1;
GO

-- Check Product_Details for product_id = 2
SELECT * FROM Product_Details WHERE product_id = 2;
GO

-- Check discount vouchers
SELECT code, start_date, end_date
FROM discount_voucher
WHERE is_deleted = 0;
GO

-- Check foreign key constraints for Order_Details
SELECT
    f.name AS foreign_key_name,
    OBJECT_NAME(f.parent_object_id) AS table_name,
    COL_NAME(fc.parent_object_id, fc.parent_column_id) AS column_name,
    OBJECT_NAME(f.referenced_object_id) AS referenced_table,
    COL_NAME(fc.referenced_object_id, fc.referenced_column_id) AS referenced_column
FROM sys.foreign_keys AS f
         INNER JOIN sys.foreign_key_columns AS fc ON f.object_id = fc.constraint_object_id
WHERE OBJECT_NAME(f.parent_object_id) = 'Order_Details';
GO

-- Update Orders total_price based on Order_Details
UPDATE Orders
SET total_price = COALESCE(
        (SELECT SUM(od.quantity * od.price)
         FROM Order_Details od
         WHERE od.order_id = Orders.id),
        0
                  );
GO
ALTER TABLE Users
    ADD face_id VARCHAR(50);
GO
