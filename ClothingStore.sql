
USE Master;
GO
DROP DATABASE IF EXISTS ACVStore;
GO
CREATE DATABASE ACVStore;
GO
USE ACVStore;
GO

-- 1. Bảng chất liệu
CREATE TABLE chat_lieu (
                           id UNIQUEIDENTIFIER PRIMARY KEY,
                           ten_chat_lieu NVARCHAR(100) NOT NULL
);

-- 2. Bảng màu sắc
CREATE TABLE mau_sac (
                         id UNIQUEIDENTIFIER PRIMARY KEY,
                         ten_mau NVARCHAR(100) NOT NULL
);

-- 3. Bảng xuất xứ
CREATE TABLE xuat_xu (
                         id UNIQUEIDENTIFIER PRIMARY KEY,
                         ten_xuat_xu NVARCHAR(100) NOT NULL
);

-- 4. Bảng kích cỡ
CREATE TABLE kich_co (
                         id UNIQUEIDENTIFIER PRIMARY KEY,
                         ten NVARCHAR(100) NOT NULL
);

-- 5. Bảng danh mục
CREATE TABLE danh_muc (
                          id UNIQUEIDENTIFIER PRIMARY KEY,
                          ten_danh_muc NVARCHAR(100) NOT NULL
);

-- 6. Bảng tay áo
CREATE TABLE tay_ao (
                        id UNIQUEIDENTIFIER PRIMARY KEY,
                        ten_tay_ao NVARCHAR(50) NOT NULL
);

-- 7. Bảng cổ áo
CREATE TABLE co_ao (
                       id UNIQUEIDENTIFIER PRIMARY KEY,
                       ten_co_ao NVARCHAR(50) NOT NULL
);

-- 8. Bảng kiểu dáng
CREATE TABLE kieu_dang (
                           id UNIQUEIDENTIFIER PRIMARY KEY,
                           ten_kieu_dang NVARCHAR(50) NOT NULL
);

-- 9. Bảng thương hiệu
CREATE TABLE thuong_hieu (
                             id UNIQUEIDENTIFIER PRIMARY KEY,
                             ten_thuong_hieu NVARCHAR(100) NOT NULL
);

-- 10. Bảng người dùng
CREATE TABLE nguoi_dung (
                            id UNIQUEIDENTIFIER PRIMARY KEY,
                            ten_dang_nhap NVARCHAR(50) NOT NULL,
                            mat_khau NVARCHAR(100) NOT NULL,
                            ho_ten NVARCHAR(100) NOT NULL,
                            email NVARCHAR(100) NOT NULL,
                            so_dien_thoai NVARCHAR(20) NOT NULL,
                            dia_chi NVARCHAR(MAX),
                            vai_tro NVARCHAR(50) CHECK (vai_tro IN (N'admin', N'customer', N'employee')),
                            ngay_sinh DATE,
                            gioi_tinh BIT,
                            tinh_thanh_pho NVARCHAR(100),
                            quan_huyen NVARCHAR(100),
                            phuong_xa NVARCHAR(100),
                            chi_tiet_dia_chi NVARCHAR(MAX),
                            thoi_gian_tao DATETIME,
                            id_qr_gioi_thieu NVARCHAR(50),
                            thoi_gian_bat_han_otp DATETIME,
                            trang_thai BIT NOT NULL
);

-- 11. Bảng phiếu giảm giá
CREATE TABLE phieu_giam_gia (
                                id UNIQUEIDENTIFIER PRIMARY KEY,
                                ten NVARCHAR(100) NOT NULL,
                                loai NVARCHAR(50) NOT NULL,
                                gia_tri_giam DECIMAL(10,2) NOT NULL,
                                gia_tri_giam_toi_thieu DECIMAL(10,2),
                                so_luong INT,
                                gioi_han_su_dung INT,
                                cong_khai BIT,
                                ngay_bat_dau DATE,
                                ngay_ket_thuc DATE,
                                thoi_gian_tao DATETIME NOT NULL,
                                kieu_phieu NVARCHAR(20) CHECK (kieu_phieu IN (N'cong_khai', N'ca_nhan')) DEFAULT N'cong_khai'
);

-- 12. Bảng phương thức thanh toán
CREATE TABLE phuong_thuc_thanh_toan (
                                        id UNIQUEIDENTIFIER PRIMARY KEY,
                                        ten_phuong_thuc NVARCHAR(100) NOT NULL,
                                        trang_thai BIT NOT NULL,
                                        ngay_tao DATETIME NOT NULL
);

-- 13. Bảng chiến dịch giảm giá
CREATE TABLE chien_dich_giam_gia (
                                     id UNIQUEIDENTIFIER PRIMARY KEY,
                                     ten NVARCHAR(100) NOT NULL,
                                     hinh_thuc_giam NVARCHAR(50) NOT NULL,
                                     so_luong INT,
                                     phan_tram_giam DECIMAL(5,2),
                                     ngay_bat_dau DATE,
                                     ngay_ket_thuc DATE,
                                     thoi_gian_tao DATETIME NOT NULL
);

-- 14. Bảng sản phẩm
CREATE TABLE san_pham (
                          id UNIQUEIDENTIFIER PRIMARY KEY,
                          id_danh_muc UNIQUEIDENTIFIER NOT NULL,
                          ma_san_pham NVARCHAR(50) UNIQUE NOT NULL,
                          ten_san_pham NVARCHAR(100) NOT NULL,
                          mo_ta NVARCHAR(MAX),
                          url_hinh_anh NVARCHAR(MAX),
                          thoi_gian_tao DATETIME NOT NULL,
                          trang_thai BIT NOT NULL,
                          FOREIGN KEY (id_danh_muc) REFERENCES danh_muc(id)
);

-- 15. Bảng chi tiết sản phẩm (Removed id_phong_cach)
CREATE TABLE chi_tiet_san_pham (
                                   id UNIQUEIDENTIFIER PRIMARY KEY,
                                   id_san_pham UNIQUEIDENTIFIER NOT NULL,
                                   id_kich_co UNIQUEIDENTIFIER NOT NULL,
                                   id_mau_sac UNIQUEIDENTIFIER NOT NULL,
                                   id_chat_lieu UNIQUEIDENTIFIER NOT NULL,
                                   id_xuat_xu UNIQUEIDENTIFIER NOT NULL,
                                   id_tay_ao UNIQUEIDENTIFIER NOT NULL,
                                   id_co_ao UNIQUEIDENTIFIER NOT NULL,
                                   id_kieu_dang UNIQUEIDENTIFIER NOT NULL,
                                   id_thuong_hieu UNIQUEIDENTIFIER NOT NULL,
                                   gia DECIMAL(10,2) NOT NULL,
                                   so_luong_ton_kho INT NOT NULL,
                                   gioi_tinh NVARCHAR(50),
                                   thoi_gian_tao DATETIME NOT NULL,
                                   trang_thai BIT NOT NULL,
                                   FOREIGN KEY (id_san_pham) REFERENCES san_pham(id) ON DELETE CASCADE,
                                   FOREIGN KEY (id_kich_co) REFERENCES kich_co(id),
                                   FOREIGN KEY (id_mau_sac) REFERENCES mau_sac(id),
                                   FOREIGN KEY (id_chat_lieu) REFERENCES chat_lieu(id),
                                   FOREIGN KEY (id_xuat_xu) REFERENCES xuat_xu(id),
                                   FOREIGN KEY (id_tay_ao) REFERENCES tay_ao(id),
                                   FOREIGN KEY (id_co_ao) REFERENCES co_ao(id),
                                   FOREIGN KEY (id_kieu_dang) REFERENCES kieu_dang(id),
                                   FOREIGN KEY (id_thuong_hieu) REFERENCES thuong_hieu(id)
);

-- 16. Bảng hình ảnh sản phẩm
CREATE TABLE hinh_anh_san_pham (
                                   id UNIQUEIDENTIFIER PRIMARY KEY,
                                   id_chi_tiet_san_pham UNIQUEIDENTIFIER NOT NULL,
                                   url_hinh_anh NVARCHAR(MAX) NOT NULL,
                                   FOREIGN KEY (id_chi_tiet_san_pham) REFERENCES chi_tiet_san_pham(id) ON DELETE CASCADE
);

-- 17. Bảng đơn hàng
CREATE TABLE don_hang (
                          id UNIQUEIDENTIFIER PRIMARY KEY,
                          id_nguoi_dung UNIQUEIDENTIFIER NOT NULL,
                          ma_don_hang NVARCHAR(50) NOT NULL,
                          trang_thai_thanh_toan BIT NOT NULL,
                          phi_van_chuyen DECIMAL(10,2),
                          id_phuong_thuc_thanh_toan UNIQUEIDENTIFIER,
                          so_tien_khach_dua DECIMAL(10,2),
                          thoi_gian_thanh_toan DATETIME,
                          thoi_gian_tao DATETIME NOT NULL,
                          tien_giam DECIMAL(10,2),
                          tong_tien DECIMAL(10,2) NOT NULL,
                          phuong_thuc_ban_hang NVARCHAR(50) NOT NULL CHECK (phuong_thuc_ban_hang IN (N'Tại quầy', N'Giao hàng')) DEFAULT N'Tại quầy',
                          FOREIGN KEY (id_nguoi_dung) REFERENCES nguoi_dung(id),
                          FOREIGN KEY (id_phuong_thuc_thanh_toan) REFERENCES phuong_thuc_thanh_toan(id)
);

-- 18. Bảng chi tiết đơn hàng
CREATE TABLE chi_tiet_don_hang (
                                   id UNIQUEIDENTIFIER PRIMARY KEY,
                                   id_don_hang UNIQUEIDENTIFIER NOT NULL,
                                   id_chi_tiet_san_pham UNIQUEIDENTIFIER NOT NULL,
                                   so_luong INT NOT NULL,
                                   gia DECIMAL(10,2) NOT NULL,
                                   ten_san_pham NVARCHAR(100) NOT NULL,
                                   thanh_tien DECIMAL(10,2) NOT NULL,
                                   ghi_chu NVARCHAR(MAX),
                                   trang_thai_hoan_tra BIT,
                                   FOREIGN KEY (id_don_hang) REFERENCES don_hang(id) ON DELETE CASCADE,
                                   FOREIGN KEY (id_chi_tiet_san_pham) REFERENCES chi_tiet_san_pham(id)
);

-- 19. Bảng hóa đơn
CREATE TABLE hoa_don (
                         id UNIQUEIDENTIFIER PRIMARY KEY,
                         id_nguoi_dung UNIQUEIDENTIFIER NOT NULL,
                         id_don_hang UNIQUEIDENTIFIER NOT NULL,
                         id_ma_giam_gia UNIQUEIDENTIFIER,
                         ngay_tao DATETIME NOT NULL,
                         ngay_thanh_toan DATETIME,
                         tong_tien DECIMAL(10,2) NOT NULL,
                         tien_giam DECIMAL(10,2),
                         id_phuong_thuc_thanh_toan UNIQUEIDENTIFIER,
                         trang_thai BIT NOT NULL,
                         ghi_chu NVARCHAR(MAX),
                         FOREIGN KEY (id_nguoi_dung) REFERENCES nguoi_dung(id),
                         FOREIGN KEY (id_don_hang) REFERENCES don_hang(id),
                         FOREIGN KEY (id_ma_giam_gia) REFERENCES chien_dich_giam_gia(id),
                         FOREIGN KEY (id_phuong_thuc_thanh_toan) REFERENCES phuong_thuc_thanh_toan(id)
);

-- 20. Bảng ticket giảm giá của người dùng
CREATE TABLE phieu_giam_gia_cua_nguoi_dung (
                                               id UNIQUEIDENTIFIER PRIMARY KEY,
                                               id_nguoi_dung UNIQUEIDENTIFIER NOT NULL,
                                               id_phieu_giam_gia UNIQUEIDENTIFIER NOT NULL,
                                               da_gui_mail BIT DEFAULT 0,
                                               FOREIGN KEY (id_nguoi_dung) REFERENCES nguoi_dung(id),
                                               FOREIGN KEY (id_phieu_giam_gia) REFERENCES phieu_giam_gia(id)
);

-- 21. Bảng chi tiết sản phẩm - chiến dịch giảm giá
CREATE TABLE chi_tiet_san_pham_chien_dich_giam_gia (
                                                       id UNIQUEIDENTIFIER PRIMARY KEY,
                                                       id_chien_dich UNIQUEIDENTIFIER NOT NULL,
                                                       id_chi_tiet_san_pham UNIQUEIDENTIFIER NOT NULL,
                                                       FOREIGN KEY (id_chien_dich) REFERENCES chien_dich_giam_gia(id),
                                                       FOREIGN KEY (id_chi_tiet_san_pham) REFERENCES chi_tiet_san_pham(id)
);

CREATE TABLE don_hang_tam (
                              id UNIQUEIDENTIFIER PRIMARY KEY,
                              id_khach_hang UNIQUEIDENTIFIER NOT NULL,
                              ma_don_hang_tam NVARCHAR(20) NOT NULL,
                              tong_tien DECIMAL(10,2) NOT NULL,
                              thoi_gian_tao DATETIME NOT NULL,
                              danh_sach_item NVARCHAR(MAX),
                              phuong_thuc_thanh_toan NVARCHAR(50),
                              phuong_thuc_ban_hang NVARCHAR(50),
                              phi_van_chuyen DECIMAL(10,2),
                              so_dien_thoai_khach_hang NVARCHAR(20),
                              id_phieu_giam_gia UNIQUEIDENTIFIER,
                              FOREIGN KEY (id_khach_hang) REFERENCES nguoi_dung(id),
                              FOREIGN KEY (id_phieu_giam_gia) REFERENCES phieu_giam_gia(id)
);


-- Insert data with explicit UUIDs
-- 1. Insert vào bảng chat_lieu
INSERT INTO chat_lieu (id, ten_chat_lieu) VALUES
                                              ('550E8400-E29B-41D4-A716-446655440000', N'Cotton'),
                                              ('550E8400-E29B-41D4-A716-446655440001', N'Polyester');

-- 2. Insert vào bảng mau_sac
INSERT INTO mau_sac (id, ten_mau) VALUES
                                      ('550E8400-E29B-41D4-A716-446655440002', N'Đen'),
                                      ('550E8400-E29B-41D4-A716-446655440003', N'Trắng'),
                                      ('550E8400-E29B-41D4-A716-446655440004', N'Xanh dương');

-- 3. Insert vào bảng xuat_xu
INSERT INTO xuat_xu (id, ten_xuat_xu) VALUES
                                          ('550E8400-E29B-41D4-A716-446655440005', N'Việt Nam'),
                                          ('550E8400-E29B-41D4-A716-446655440006', N'Trung Quốc');

-- 4. Insert vào bảng kich_co
INSERT INTO kich_co (id, ten) VALUES
                                  ('550E8400-E29B-41D4-A716-446655440009', N'S'),
                                  ('550E8400-E29B-41D4-A716-446655440010', N'M'),
                                  ('550E8400-E29B-41D4-A716-446655440011', N'L');

-- 5. Insert vào bảng danh_muc
INSERT INTO danh_muc (id, ten_danh_muc) VALUES
    ('550E8400-E29B-41D4-A716-446655440012', N'Áo thun nam');

-- 6. Insert vào bảng tay_ao
INSERT INTO tay_ao (id, ten_tay_ao) VALUES
    ('550E8400-E29B-41D4-A716-446655440032', N'Ngắn');

-- 7. Insert vào bảng co_ao
INSERT INTO co_ao (id, ten_co_ao) VALUES
    ('550E8400-E29B-41D4-A716-446655440033', N'Cổ tròn');

-- 8. Insert vào bảng kieu_dang
INSERT INTO kieu_dang (id, ten_kieu_dang) VALUES
                                              ('550E8400-E29B-41D4-A716-446655440034', N'Rộng'),
                                              ('550E8400-E29B-41D4-A716-446655440035', N'Ôm');

-- 9. Insert vào bảng thuong_hieu
INSERT INTO thuong_hieu (id, ten_thuong_hieu) VALUES
                                                  ('550E8400-E29B-41D4-A716-446655440036', N'Uniqlo'),
                                                  ('550E8400-E29B-41D4-A716-446655440037', N'Adidas');

-- 10. Insert vào bảng nguoi_dung
INSERT INTO nguoi_dung (id, ten_dang_nhap, mat_khau, ho_ten, email, so_dien_thoai, dia_chi, vai_tro, ngay_sinh, gioi_tinh, tinh_thanh_pho, quan_huyen, phuong_xa, chi_tiet_dia_chi, thoi_gian_tao, id_qr_gioi_thieu, thoi_gian_bat_han_otp, trang_thai) VALUES
                                                                                                                                                                                                                                                            ('550E8400-E29B-41D4-A716-446655440013', N'admin', N'admin123', N'Nguyễn Văn Admin', N'admin1@example.com', N'0901234567', N'123 Đường ABC, Hà Nội', N'admin', '1985-01-01', 1, N'Hà Nội', N'Cầu Giấy', N'Dịch Vọng', N'Số 123, Đường ABC', GETDATE(), N'QR123', NULL, 1),
                                                                                                                                                                                                                                                            ('550E8400-E29B-41D4-A716-446655440014', N'customer1', N'customer123', N'Trần Thị Customer', N'customer1@example.com', N'0912345678', N'456 Đường XYZ, TP.HCM', N'customer', '1995-05-10', 0, N'TP.HCM', N'Quận 1', N'Bến Nghé', N'Số 456, Đường XYZ', GETDATE(), N'QR456', NULL, 1),
                                                                                                                                                                                                                                                            ('550E8400-E29B-41D4-A716-446655440015', N'employee1', N'employee123', N'Lê Văn Employee', N'employee1@example.com', N'0923456789', N'789 Đường KLM, Đà Nẵng', N'employee', '1990-03-15', 1, N'Đà Nẵng', N'Hải Châu', N'Hải Châu I', N'Số 789, Đường KLM', GETDATE(), N'QR789', NULL, 1);

-- 11. Insert vào bảng phieu_giam_gia
INSERT INTO phieu_giam_gia (id, ten, loai, gia_tri_giam, gia_tri_giam_toi_thieu, so_luong, gioi_han_su_dung, cong_khai, ngay_bat_dau, ngay_ket_thuc, thoi_gian_tao, kieu_phieu) VALUES
    ('550E8400-E29B-41D4-A716-446655440016', N'Giảm 10% áo nam', N'Phần trăm', 10.00, 500000.00, 100, 1, 1, '2025-05-24', '2025-06-24', GETDATE(), N'cong_khai');

-- 12. Insert vào bảng phuong_thuc_thanh_toan
INSERT INTO phuong_thuc_thanh_toan (id, ten_phuong_thuc, trang_thai, ngay_tao) VALUES
                                                                                   ('550E8400-E29B-41D4-A716-446655440017', N'Tiền mặt', 1, GETDATE()),
                                                                                   ('550E8400-E29B-41D4-A716-446655440018', N'Chuyển khoản', 1, GETDATE());

-- 13. Insert vào bảng chien_dich_giam_gia
INSERT INTO chien_dich_giam_gia (id, ten, hinh_thuc_giam, so_luong, phan_tram_giam, ngay_bat_dau, ngay_ket_thuc, thoi_gian_tao) VALUES
    ('550E8400-E29B-41D4-A716-446655440019', N'Sale áo nam mùa hè', N'Phần trăm', 100, 20.00, '2025-06-01', '2025-06-30', GETDATE());

-- 14. Insert vào bảng san_pham
INSERT INTO san_pham (id, id_danh_muc, ten_san_pham, ma_san_pham, mo_ta, url_hinh_anh, thoi_gian_tao, trang_thai) VALUES
                                                                                                                      ('550E8400-E29B-41D4-A716-446655440020', '550E8400-E29B-41D4-A716-446655440012', N'Áo thun nam cổ tròn', N'SP001', N'Áo thun cotton thoáng mát, phong cách hiện đại', N'https://img.muji.net/img/item/4550344421994_01_400.jpg', GETDATE(), 1),
                                                                                                                      ('550E8400-E29B-41D4-A716-446655440021', '550E8400-E29B-41D4-A716-446655440012', N'Áo thun nam thể thao', N'SP002', N'Áo thun cotton co giãn, phù hợp hoạt động thể thao', N'https://img.muji.net/img/item/4550512218142_1260.jpg', GETDATE(), 1);

-- 15. Insert vào bảng chi_tiet_san_pham (Removed id_phong_cach)
INSERT INTO chi_tiet_san_pham (id, id_san_pham, id_kich_co, id_mau_sac, id_chat_lieu, id_xuat_xu, id_tay_ao, id_co_ao, id_kieu_dang, id_thuong_hieu, gia, so_luong_ton_kho, gioi_tinh, thoi_gian_tao, trang_thai) VALUES
                                                                                                                                                                                                                      ('550E8400-E29B-41D4-A716-446655440022', '550E8400-E29B-41D4-A716-446655440020', '550E8400-E29B-41D4-A716-446655440009', '550E8400-E29B-41D4-A716-446655440002', '550E8400-E29B-41D4-A716-446655440000', '550E8400-E29B-41D4-A716-446655440005', '550E8400-E29B-41D4-A716-446655440032', '550E8400-E29B-41D4-A716-446655440033', '550E8400-E29B-41D4-A716-446655440034', '550E8400-E29B-41D4-A716-446655440036', 100000.00, 100, N'Nam', GETDATE(), 1),
                                                                                                                                                                                                                      ('550E8400-E29B-41D4-A716-446655440023', '550E8400-E29B-41D4-A716-446655440020', '550E8400-E29B-41D4-A716-446655440010', '550E8400-E29B-41D4-A716-446655440003', '550E8400-E29B-41D4-A716-446655440000', '550E8400-E29B-41D4-A716-446655440005', '550E8400-E29B-41D4-A716-446655440032', '550E8400-E29B-41D4-A716-446655440033', '550E8400-E29B-41D4-A716-446655440034', '550E8400-E29B-41D4-A716-446655440036', 120000.00, 150, N'Nam', GETDATE(), 1),
                                                                                                                                                                                                                      ('550E8400-E29B-41D4-A716-446655440024', '550E8400-E29B-41D4-A716-446655440021', '550E8400-E29B-41D4-A716-446655440011', '550E8400-E29B-41D4-A716-446655440004', '550E8400-E29B-41D4-A716-446655440000', '550E8400-E29B-41D4-A716-446655440005', '550E8400-E29B-41D4-A716-446655440032', '550E8400-E29B-41D4-A716-446655440033', '550E8400-E29B-41D4-A716-446655440035', '550E8400-E29B-41D4-A716-446655440037', 110000.00, 80, N'Nam', GETDATE(), 1);

-- 16. Insert vào bảng hinh_anh_san_pham
INSERT INTO hinh_anh_san_pham (id, id_chi_tiet_san_pham, url_hinh_anh) VALUES
                                                                           ('550E8400-E29B-41D4-A716-446655440025', '550E8400-E29B-41D4-A716-446655440022', N'https://img.muji.net/img/item/4550344421994_01_400.jpg'),
                                                                           ('550E8400-E29B-41D4-A716-446655440026', '550E8400-E29B-41D4-A716-446655440023', N'https://img.muji.net/img/item/4550512218142_1260.jpg'),
                                                                           ('550E8400-E29B-41D4-A716-446655440027', '550E8400-E29B-41D4-A716-446655440024', N'https://pos.nvncdn.com/5048a3-93414/ps/20220323_U27FWOyu1YcXxbMujIWbMrnS.jpg');

-- 17. Insert vào bảng don_hang
INSERT INTO don_hang (id, id_nguoi_dung, ma_don_hang, trang_thai_thanh_toan, phi_van_chuyen, id_phuong_thuc_thanh_toan, so_tien_khach_dua, thoi_gian_thanh_toan, thoi_gian_tao, tien_giam, tong_tien, phuong_thuc_ban_hang) VALUES
    ('550E8400-E29B-41D4-A716-446655440028', '550E8400-E29B-41D4-A716-446655440014', N'DH001', 1, 30000.00, '550E8400-E29B-41D4-A716-446655440017', 230000.00, GETDATE(), GETDATE(), 20000.00, 230000.00, N'Giao hàng');

-- 18. Insert vào bảng chi_tiet_don_hang
INSERT INTO chi_tiet_don_hang (id, id_don_hang, id_chi_tiet_san_pham, so_luong, gia, ten_san_pham, thanh_tien, ghi_chu, trang_thai_hoan_tra) VALUES
    ('550E8400-E29B-41D4-A716-446655440029', '550E8400-E29B-41D4-A716-446655440028', '550E8400-E29B-41D4-A716-446655440022', 2, 100000.00, N'Áo thun nam cổ tròn', 200000.00, N'Kích cỡ S, màu đen', 0);

-- 19. Insert vào bảng hoa_don
INSERT INTO hoa_don (id, id_nguoi_dung, id_don_hang, id_ma_giam_gia, ngay_tao, ngay_thanh_toan, tong_tien, tien_giam, id_phuong_thuc_thanh_toan, trang_thai, ghi_chu) VALUES
    ('550E8400-E29B-41D4-A716-446655440030', '550E8400-E29B-41D4-A716-446655440014', '550E8400-E29B-41D4-A716-446655440028', '550E8400-E29B-41D4-A716-446655440019', GETDATE(), GETDATE(), 230000.00, 20000.00, '550E8400-E29B-41D4-A716-446655440017', 1, N'Hoàn tất');

-- 20. Insert vào bảng phieu_giam_gia_cua_nguoi_dung
INSERT INTO phieu_giam_gia_cua_nguoi_dung (id, id_nguoi_dung, id_phieu_giam_gia, da_gui_mail) VALUES
    ('550E8400-E29B-41D4-A716-446655440031', '550E8400-E29B-41D4-A716-446655440014', '550E8400-E29B-41D4-A716-446655440016', 0);

-- 21. Insert vào bảng chi_tiet_san_pham_chien_dich_giam_gia
INSERT INTO chi_tiet_san_pham_chien_dich_giam_gia (id, id_chien_dich, id_chi_tiet_san_pham) VALUES
                                                                                                (NEWID(), '550E8400-E29B-41D4-A716-446655440019', '550E8400-E29B-41D4-A716-446655440022'),
                                                                                                (NEWID(), '550E8400-E29B-41D4-A716-446655440019', '550E8400-E29B-41D4-A716-446655440023');

-- Select data from all tables
SELECT * FROM chat_lieu;
SELECT * FROM mau_sac;
SELECT * FROM xuat_xu;
SELECT * FROM kich_co;
SELECT * FROM danh_muc;
SELECT * FROM tay_ao;
SELECT * FROM co_ao;
SELECT * FROM kieu_dang;
SELECT * FROM thuong_hieu;
SELECT * FROM nguoi_dung;
SELECT * FROM phieu_giam_gia;
SELECT * FROM phuong_thuc_thanh_toan;
SELECT * FROM chien_dich_giam_gia;
SELECT * FROM san_pham;
SELECT * FROM chi_tiet_san_pham;
SELECT * FROM hinh_anh_san_pham;
SELECT * FROM don_hang;
SELECT * FROM chi_tiet_don_hang;
SELECT * FROM hoa_don;
SELECT * FROM phieu_giam_gia_cua_nguoi_dung;
SELECT * FROM chi_tiet_san_pham_chien_dich_giam_gia;
ALTER TABLE phieu_giam_gia ADD gia_tri_giam_toi_da DECIMAL(10, 2);
ALTER TABLE phieu_giam_gia ADD ma VARCHAR(50);
ALTER TABLE chien_dich_giam_gia
    ADD ma NVARCHAR(50) NOT NULL DEFAULT NEWID();
INSERT INTO chien_dich_giam_gia (id, ma, ten, hinh_thuc_giam, so_luong, phan_tram_giam, ngay_bat_dau, ngay_ket_thuc, thoi_gian_tao)
VALUES (
           NEWID(),
           'KM001',
           N'Giảm giá hè 2025',
           N'Phần trăm',
           100,
           15.0,
           '2025-06-01',
           '2025-06-30',
           GETDATE()
       );





SELECT id, ho_ten, email, vai_tro, trang_thai FROM nguoi_dung WHERE vai_tro = 'CUSTOMER';
INSERT INTO nguoi_dung (
    id, ten_dang_nhap, mat_khau, ho_ten, email, so_dien_thoai, dia_chi,
    vai_tro, ngay_sinh, gioi_tinh, tinh_thanh_pho, quan_huyen, phuong_xa,
    chi_tiet_dia_chi, thoi_gian_tao, id_qr_gioi_thieu, thoi_gian_bat_han_otp, trang_thai
)
VALUES
    (NEWID(), N'khachle', N'khachle', N'Khách lẻ', 'khachle@example.com', '0999999999', N'khách lẻ', 'customer', '2000-05-05', 0, N'Hải Phòng', N'Lê Chân', N'An Biên', N'Số 5, Phố E', GETDATE(), NULL, NULL, 1),
    (NEWID(), N'customer6', N'123456', N'Hoàng Hải Nam', 'namhaihoang3103@gmail.com', '0955555555', N'654 JKL, Hải Phòng', 'customer', '2000-05-05', 0, N'Hải Phòng', N'Lê Chân', N'An Biên', N'Số 5, Phố E', GETDATE(), NULL, NULL, 1),

    (NEWID(), N'customer2', N'123456', N'Nguyễn Thị A', 'customer2@example.com', '0911111111', N'123 ABC, Hà Nội', 'customer', '1990-01-01', 0, N'Hà Nội', N'Ba Đình', N'Phúc Xá', N'Số 1, Phố A', GETDATE(), NULL, NULL, 1),
    (NEWID(), N'customer3', N'123456', N'Lê Văn B', 'customer3@example.com', '0922222222', N'456 XYZ, TP.HCM', 'customer', '1992-02-02', 1, N'TP.HCM', N'Quận 5', N'Phường 5', N'Số 2, Phố B', GETDATE(), NULL, NULL, 1),
    (NEWID(), N'customer4', N'123456', N'Trần Thị C', 'customer4@example.com', '0933333333', N'789 DEF, Đà Nẵng', 'customer', '1995-03-03', 0, N'Đà Nẵng', N'Hải Châu', N'Thanh Bình', N'Số 3, Phố C', GETDATE(), NULL, NULL, 1),
    (NEWID(), N'customer5', N'123456', N'Phạm Văn D', 'customer5@example.com', '0944444444', N'321 GHI, Cần Thơ', 'customer', '1998-04-04', 1, N'Cần Thơ', N'Ninh Kiều', N'An Cư', N'Số 4, Phố D', GETDATE(), NULL, NULL, 1),
    (NEWID(), N'customer6', N'123456', N'Hoàng Thị E', 'customer6@example.com', '0955555555', N'654 JKL, Hải Phòng', 'customer', '2000-05-05', 0, N'Hải Phòng', N'Lê Chân', N'An Biên', N'Số 5, Phố E', GETDATE(), NULL, NULL, 1);
ALTER TABLE phieu_giam_gia_cua_nguoi_dung
    ADD so_luot_con_lai INT DEFAULT 0;
ALTER TABLE phieu_giam_gia_cua_nguoi_dung ADD so_luot_duoc_su_dung INT DEFAULT 1;

select * from chi_tiet_san_pham where id_san_pham = '550e8400-e29b-41d4-a716-446655440021'
select * from mau_sac where id = '550e8400-e29b-41d4-a716-446655440004'
select * from kich_co where id = '550e8400-e29b-41d4-a716-446655440011'
SELECT * FROM chi_tiet_san_pham
WHERE id_san_pham = '550e8400-e29b-41d4-a716-446655440020';
select * from don_hang
select * from nguoi_dung

-- INSERT INTO phieu_giam_gia_cua_nguoi_dung (id, id_phieu_giam_gia, id_nguoi_dung, so_luot_con_lai, da_gui_mail)
-- VALUES (NEWID(), '5aa57234-8476-451a-a008-1a4128682646',
--         (SELECT id FROM nguoi_dung WHERE so_dien_thoai = '0999999999'), 1, 0);

SELECT id, id_phieu_giam_gia, id_nguoi_dung, so_luot_con_lai
FROM phieu_giam_gia_cua_nguoi_dung
WHERE id_phieu_giam_gia = '5aa57234-8476-451a-a008-1a4128682646'
  AND id_nguoi_dung = (SELECT id FROM nguoi_dung WHERE so_dien_thoai = '0999999999');
update phieu_giam_gia
set so_luong = 100 WHERE id = '5aa57234-8476-451a-a008-1a4128682646';

SELECT * FROM mau_sac WHERE id = '550e8400-e29b-41d4-a716-446655440002';
SELECT * FROM kich_co WHERE id = '550e8400-e29b-41d4-a716-446655440009';

SELECT * FROM san_pham WHERE id = '550e8400-e29b-41d4-a716-446655440021';
SELECT * FROM san_pham WHERE id = '558e8400-e29b-41d4-a716-466554480021';

SELECT * FROM chi_tiet_san_pham WHERE id = '550e8400-e29b-41d4-a716-446655440022';
SELECT * FROM phuong_thuc_thanh_toan;
select * from nguoi_dung

SELECT * FROM don_hang WHERE ma_don_hang = 'DHB098971';
SELECT * FROM don_hang_tam
SELECT id, danh_sach_item
FROM don_hang_tam
WHERE id = 'c554b5f1-8b99-48b3-8390-1d91bc302d9e';
SELECT * FROM san_pham WHERE id IN ('550e8400-e29b-41d4-a716-446655440040', '550e8400-e29b-41d4-a716-446655440041', '550e8400-e29b-41d4-a716-446655440042');
SELECT * FROM chi_tiet_san_pham where id_san_pham = '550e8400-e29b-41d4-a716-446655440040'
    INSERT INTO san_pham (id, id_danh_muc, ten_san_pham, ma_san_pham, mo_ta, url_hinh_anh, thoi_gian_tao, trang_thai) VALUES
    ('550e8400-e29b-41d4-a716-446655440040', '550e8400-e29b-41d4-a716-446655440012', N'Áo sơ mi nam dài tay', N'SP003', N'Áo sơ mi cotton cao cấp, phù hợp mặc công sở', N'https://example.com/shirt1.jpg', GETDATE(), 1),
    ('550e8400-e29b-41d4-a716-446655440041', '550e8400-e29b-41d4-a716-446655440012', N'Áo hoodie nam mùa đông', N'SP004', N'Áo hoodie giữ ấm, chất liệu polyester', N'https://example.com/hoodie1.jpg', GETDATE(), 1),
    ('550e8400-e29b-41d4-a716-446655440042', '550e8400-e29b-41d4-a716-446655440012', N'Áo khoác nam chống nước', N'SP005', N'Áo khoác chống nước, phong cách thể thao', N'https://example.com/jacket1.jpg', GETDATE(), 1);

INSERT INTO chi_tiet_san_pham (id, id_san_pham, id_kich_co, id_mau_sac, id_chat_lieu, id_xuat_xu, id_tay_ao, id_co_ao, id_kieu_dang, id_thuong_hieu, gia, so_luong_ton_kho, gioi_tinh, thoi_gian_tao, trang_thai) VALUES
                                                                                                                                                                                                                      ('550e8400-e29b-41d4-a716-446655440043', '550e8400-e29b-41d4-a716-446655440040', '550e8400-e29b-41d4-a716-446655440009', '550e8400-e29b-41d4-a716-446655440002', '550e8400-e29b-41d4-a716-446655440000', '550e8400-e29b-41d4-a716-446655440005', '550e8400-e29b-41d4-a716-446655440032', '550e8400-e29b-41d4-a716-446655440033', '550e8400-e29b-41d4-a716-446655440034', '550e8400-e29b-41d4-a716-446655440036', 250000.00, 50, N'Nam', GETDATE(), 1),
                                                                                                                                                                                                                      ('550e8400-e29b-41d4-a716-446655440044', '550e8400-e29b-41d4-a716-446655440041', '550e8400-e29b-41d4-a716-446655440010', '550e8400-e29b-41d4-a716-446655440003', '550e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440006', '550e8400-e29b-41d4-a716-446655440032', '550e8400-e29b-41d4-a716-446655440033', '550e8400-e29b-41d4-a716-446655440035', '550e8400-e29b-41d4-a716-446655440037', 300000.00, 30, N'Nam', GETDATE(), 1),
                                                                                                                                                                                                                      ('550e8400-e29b-41d4-a716-446655440045', '550e8400-e29b-41d4-a716-446655440042', '550e8400-e29b-41d4-a716-446655440011', '550e8400-e29b-41d4-a716-446655440004', '550e8400-e29b-41d4-a716-446655440000', '550e8400-e29b-41d4-a716-446655440005', '550e8400-e29b-41d4-a716-446655440032', '550e8400-e29b-41d4-a716-446655440033', '550e8400-e29b-41d4-a716-446655440034', '550e8400-e29b-41d4-a716-446655440036', 400000.00, 20, N'Nam', GETDATE(), 1);

INSERT INTO hinh_anh_san_pham (id, id_chi_tiet_san_pham, url_hinh_anh) VALUES
                                                                           ('550e8400-e29b-41d4-a716-446655440046', '550e8400-e29b-41d4-a716-446655440043', N'https://example.com/shirt1.jpg'),
                                                                           ('550e8400-e29b-41d4-a716-446655440047', '550e8400-e29b-41d4-a716-446655440044', N'https://example.com/hoodie1.jpg'),
                                                                           ('550e8400-e29b-41d4-a716-446655440048', '550e8400-e29b-41d4-a716-446655440045', N'https://example.com/jacket1.jpg');

select * from nguoi_dung
SELECT * FROM san_pham WHERE id = '550e8400-e29b-41d4-a716-446655440020';
select * from don_hang_tam
select * from san_pham

CREATE TABLE thong_ke_doanh_thu_chi_tiet (
                                             id UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(),
                                             ngay_thanh_toan DATE NOT NULL,
                                             id_hoa_don UNIQUEIDENTIFIER NOT NULL,
                                             id_chi_tiet_san_pham UNIQUEIDENTIFIER NOT NULL,
                                             id_san_pham UNIQUEIDENTIFIER NOT NULL,
                                             ten_san_pham NVARCHAR(100) NOT NULL,
                                             kich_co NVARCHAR(50) NOT NULL,
                                             mau_sac NVARCHAR(50) NOT NULL,
                                             so_luong_da_ban INT NOT NULL DEFAULT 0,
                                             doanh_thu NUMERIC(38,2) NOT NULL DEFAULT 0.00,
                                             so_luong_ton_kho INT NOT NULL DEFAULT 0,
                                             image_url NVARCHAR(MAX),
                                             FOREIGN KEY (id_hoa_don) REFERENCES hoa_don(id),
                                             FOREIGN KEY (id_chi_tiet_san_pham) REFERENCES chi_tiet_san_pham(id),
                                             FOREIGN KEY (id_san_pham) REFERENCES san_pham(id)
);
SELECT id, ho_ten, email FROM nguoi_dung WHERE id = 'a6295043-02c3-457a-b484-02af5152358d';
select * from nguoi_dung
UPDATE nguoi_dung
SET email = 'namhaihoang3103@gmail.com'
WHERE id = 'a6295043-02c3-457a-b484-02af5152358d';

UPDATE nguoi_dung
SET vai_tro = UPPER(vai_tro)
WHERE vai_tro IN ('admin', 'employee', 'customer');