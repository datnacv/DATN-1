
USE Master;
GO
DROP DATABASE IF EXISTS ACVStore;
GO
CREATE DATABASE ACVStore
COLLATE Vietnamese_100_CI_AS_SC_UTF8;
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
                            ten_dang_nhap NVARCHAR(50),
                            mat_khau NVARCHAR(100),
                            ho_ten NVARCHAR(100) NOT NULL,
                            email NVARCHAR(100) NOT NULL,
                            so_dien_thoai NVARCHAR(20) UNIQUE,
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
                          phuong_thuc_ban_hang NVARCHAR(50) NOT NULL CHECK (phuong_thuc_ban_hang IN (N'Tại quầy', N'Giao hàng', N'Online')) DEFAULT N'Tại quầy',
                          FOREIGN KEY (id_nguoi_dung) REFERENCES nguoi_dung(id),
                          FOREIGN KEY (id_phuong_thuc_thanh_toan) REFERENCES phuong_thuc_thanh_toan(id)
);

select * from don_hang where ma_don_hang = 'DH05764A27'

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
                                   ly_do_tra_hang NVARCHAR(MAX),
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
                         trang_thai NVARCHAR(50) NOT NULL,
                         ghi_chu NVARCHAR(MAX),
                         FOREIGN KEY (id_nguoi_dung) REFERENCES nguoi_dung(id),
                         FOREIGN KEY (id_don_hang) REFERENCES don_hang(id),
                         FOREIGN KEY (id_ma_giam_gia) REFERENCES chien_dich_giam_gia(id),
                         FOREIGN KEY (id_phuong_thuc_thanh_toan) REFERENCES phuong_thuc_thanh_toan(id)
);


CREATE TABLE lich_su_hoa_don (
                                 id UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(),
                                 id_hoa_don UNIQUEIDENTIFIER NOT NULL,
                                 trang_thai NVARCHAR(50) NOT NULL,
                                 thoi_gian DATETIME NOT NULL,
                                 ghi_chu NVARCHAR(MAX),
                                 FOREIGN KEY (id_hoa_don) REFERENCES hoa_don(id)
);

CREATE TABLE lich_su_tra_hang (
                                  id UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(),
                                  id_chi_tiet_don_hang UNIQUEIDENTIFIER NOT NULL,
                                  id_hoa_don UNIQUEIDENTIFIER NOT NULL,
                                  so_luong INT NOT NULL,
                                  tong_tien_hoan DECIMAL(10,2) NOT NULL,
                                  ly_do_tra_hang NVARCHAR(MAX),
                                  thoi_gian_tra DATETIME NOT NULL DEFAULT GETDATE(),
                                  trang_thai NVARCHAR(50) NOT NULL DEFAULT N'Đã trả',
                                  FOREIGN KEY (id_chi_tiet_don_hang) REFERENCES chi_tiet_don_hang(id),
                                  FOREIGN KEY (id_hoa_don) REFERENCES hoa_don(id)
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

-- gio hang
CREATE TABLE gio_hang (
                          id UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(),
                          id_nguoi_dung UNIQUEIDENTIFIER NOT NULL,
                          ma_gio_hang NVARCHAR(20) NOT NULL,
                          tong_tien DECIMAL(15, 2) DEFAULT 0,
                          thoi_gian_tao DATETIME NOT NULL DEFAULT GETDATE(),
                          trang_thai BIT NOT NULL DEFAULT 1,
                          FOREIGN KEY (id_nguoi_dung) REFERENCES nguoi_dung(id)
);

CREATE TABLE gio_hang_chi_tiet (
                                   id UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(),
                                   id_gio_hang UNIQUEIDENTIFIER NOT NULL,
                                   id_chi_tiet_san_pham UNIQUEIDENTIFIER NOT NULL,
                                   so_luong INT NOT NULL DEFAULT 1,
                                   gia DECIMAL(10, 2) NOT NULL,
                                   tien_giam DECIMAL(10, 2) DEFAULT 0,
                                   ghi_chu NVARCHAR(MAX),
                                   thoi_gian_them DATETIME NOT NULL DEFAULT GETDATE(),
                                   trang_thai BIT NOT NULL DEFAULT 1,
                                   FOREIGN KEY (id_gio_hang) REFERENCES gio_hang(id) ON DELETE CASCADE,
                                   FOREIGN KEY (id_chi_tiet_san_pham) REFERENCES chi_tiet_san_pham(id)
);
CREATE TABLE thong_ke_doanh_thu_chi_tiet (
                                             id UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(),
                                             ngay_thanh_toan DATE NOT NULL,
                                             id_chi_tiet_don_hang UNIQUEIDENTIFIER NOT NULL,
                                             id_chi_tiet_san_pham UNIQUEIDENTIFIER NOT NULL,
                                             id_san_pham UNIQUEIDENTIFIER NOT NULL,
                                             ten_san_pham NVARCHAR(100) NOT NULL,
                                             kich_co NVARCHAR(50) NOT NULL,
                                             mau_sac NVARCHAR(50) NOT NULL,
                                             so_luong_da_ban INT NOT NULL DEFAULT 0,
                                             doanh_thu NUMERIC(38,2) NOT NULL DEFAULT 0.00,
                                             so_luong_ton_kho INT NOT NULL DEFAULT 0,
                                             image_url NVARCHAR(MAX),
                                             FOREIGN KEY (id_chi_tiet_don_hang) REFERENCES chi_tiet_don_hang(id) ON DELETE CASCADE,
                                             FOREIGN KEY (id_chi_tiet_san_pham) REFERENCES chi_tiet_san_pham(id),
                                             FOREIGN KEY (id_san_pham) REFERENCES san_pham(id)
);
CREATE TABLE thong_bao_nhom (
                                id UNIQUEIDENTIFIER PRIMARY KEY,
                                id_don_hang UNIQUEIDENTIFIER NOT NULL,
                                vai_tro_nhan NVARCHAR(50) NOT NULL CHECK (vai_tro_nhan IN (N'admin', N'employee', N'customer')),
                                tieu_de NVARCHAR(200) NOT NULL,
                                noi_dung NVARCHAR(MAX) NOT NULL,
                                thoi_gian_tao DATETIME NOT NULL,
                                trang_thai NVARCHAR(50) NOT NULL DEFAULT N'Mới',
                                FOREIGN KEY (id_don_hang) REFERENCES don_hang(id) ON DELETE CASCADE
);

-- Tạo bảng chi_tiet_thong_bao_nhom
CREATE TABLE chi_tiet_thong_bao_nhom (
                                         id UNIQUEIDENTIFIER PRIMARY KEY,
                                         id_thong_bao_nhom UNIQUEIDENTIFIER NOT NULL,
                                         id_nguoi_dung UNIQUEIDENTIFIER NOT NULL,
                                         da_xem BIT NOT NULL DEFAULT 0,
                                         FOREIGN KEY (id_thong_bao_nhom) REFERENCES thong_bao_nhom(id) ON DELETE CASCADE,
                                         FOREIGN KEY (id_nguoi_dung) REFERENCES nguoi_dung(id)
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
INSERT INTO nguoi_dung (id, ten_dang_nhap, mat_khau, ho_ten, email, so_dien_thoai, vai_tro, ngay_sinh, gioi_tinh, tinh_thanh_pho, quan_huyen, phuong_xa, chi_tiet_dia_chi, thoi_gian_tao, id_qr_gioi_thieu, thoi_gian_bat_han_otp, trang_thai) VALUES
                                                                                                                                                                                                                                                   ('550E8400-E29B-41D4-A716-446655440013', N'admin', N'admin123', N'Nguyễn Văn Admin', N'admin1@example.com', N'0901234567', N'admin', '1985-01-01', 1, N'Hà Nội', N'Cầu Giấy', N'Dịch Vọng', N'Số 123, Đường ABC', GETDATE(), N'QR123', NULL, 1),
                                                                                                                                                                                                                                                   ('550E8400-E29B-41D4-A716-446655440014', N'customer1', N'customer123', N'Trần Thị Customer', N'customer1@example.com', N'0912345678', N'customer', '1995-05-10', 0, N'TP.HCM', N'Quận 1', N'Bến Nghé', N'Số 456, Đường XYZ', GETDATE(), N'QR456', NULL, 1),
                                                                                                                                                                                                                                                   ('550E8400-E29B-41D4-A716-446655440015', N'employee1', N'employee123', N'Lê Văn Employee', N'employee1@example.com', N'0923456789', N'employee', '1990-03-15', 1, N'Đà Nẵng', N'Hải Châu', N'Hải Châu I', N'Số 789, Đường KLM', GETDATE(), N'QR789', NULL, 1);
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
INSERT INTO hoa_don (
    id,
    id_nguoi_dung,
    id_don_hang,
    id_ma_giam_gia,
    ngay_tao,
    ngay_thanh_toan,
    tong_tien,
    tien_giam,
    id_phuong_thuc_thanh_toan,
    trang_thai,
    ghi_chu
) VALUES (
             '550E8400-E29B-41D4-A716-446655440030',
             '550E8400-E29B-41D4-A716-446655440014',
             '550E8400-E29B-41D4-A716-446655440028',
             '550E8400-E29B-41D4-A716-446655440019',
             GETDATE(),
             GETDATE(),
             230000.00,
             20000.00,
             '550E8400-E29B-41D4-A716-446655440017',
             N'Hoàn thành',
             N'Hoàn thành'
         );


-- 20. Insert vào bảng phieu_giam_gia_cua_nguoi_dung
INSERT INTO phieu_giam_gia_cua_nguoi_dung (id, id_nguoi_dung, id_phieu_giam_gia, da_gui_mail) VALUES
    ('550E8400-E29B-41D4-A716-446655440031', '550E8400-E29B-41D4-A716-446655440014', '550E8400-E29B-41D4-A716-446655440016', 0);


ALTER TABLE don_hang ADD ghi_chu NVARCHAR(MAX);
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



INSERT INTO nguoi_dung (id, ten_dang_nhap, mat_khau, ho_ten, email, so_dien_thoai, vai_tro, ngay_sinh, gioi_tinh, tinh_thanh_pho, quan_huyen, phuong_xa, chi_tiet_dia_chi, thoi_gian_tao, id_qr_gioi_thieu, thoi_gian_bat_han_otp, trang_thai) VALUES
                                                                                                                                                                                                                                                   (NEWID(), N'adminlong', N'admin123', N'Phạm Đức Long', 'duclong0910@gmail.com', '0911006045', 'admin', '2000-05-05', 0, N'Hải Phòng', N'Lê Chân', N'An Biên', N'Số 5, Phố E', GETDATE(), NULL, NULL, 1),
                                                                                                                                                                                                                                                   (NEWID(), N'adminluc', N'admin123', N'Nguyễn Xuân Lực', 'nguyenxuanlucthanhoai@gmail.com', '0866716384', 'admin', '2000-05-05', 0, N'Hải Phòng', N'Lê Chân', N'An Biên', N'Số 5, Phố E', GETDATE(), NULL, NULL, 1),
                                                                                                                                                                                                                                                   (NEWID(), N'adminmanh', N'admin123', N'Phạm Duy Mạnh', 'pdm25122006@gmail.com', '0358187642', 'admin', '2000-05-05', 0, N'Hải Phòng', N'Lê Chân', N'An Biên', N'Số 5, Phố E', GETDATE(), NULL, NULL, 1),
                                                                                                                                                                                                                                                   (NEWID(), N'adminnam', N'admin123', N'Hoàng Hải Nam', 'namhaihoang3103@gmail.com', '0969469018', 'admin', '2000-05-05', 0, N'Hải Phòng', N'Lê Chân', N'An Biên', N'Số 5, Phố E', GETDATE(), NULL, NULL, 1),
                                                                                                                                                                                                                                                   (NEWID(), N'adminsy', N'admin123', N'Sỹ Lê Minh Hiếu', 'sy@gmail.com', '0978790099', 'admin', '2000-05-05', 0, N'Hải Phòng', N'Lê Chân', N'An Biên', N'Số 5, Phố E', GETDATE(), NULL, NULL, 1),
                                                                                                                                                                                                                                                   (NEWID(), N'khachle', N'khachle', N'Khách lẻ', 'khachle@example.com', '0999999999', 'customer', '2000-05-05', 0, N'Hải Phòng', N'Lê Chân', N'An Biên', N'Số 5, Phố E', GETDATE(), NULL, NULL, 1),
                                                                                                                                                                                                                                                   (NEWID(), N'customer2', N'123456', N'Nguyễn Thị A', 'customer2@example.com', '0911111111', 'customer', '1990-01-01', 0, N'Hà Nội', N'Ba Đình', N'Phúc Xá', N'Số 1, Phố A', GETDATE(), NULL, NULL, 1),
                                                                                                                                                                                                                                                   (NEWID(), N'customer3', N'123456', N'Lê Văn B', 'customer3@example.com', '0922222222', 'customer', '1992-02-02', 1, N'TP.HCM', N'Quận 5', N'Phường 5', N'Số 2, Phố B', GETDATE(), NULL, NULL, 1),
                                                                                                                                                                                                                                                   (NEWID(), N'customer4', N'123456', N'Trần Thị C', 'customer4@example.com', '0933333333', 'customer', '1995-03-03', 0, N'Đà Nẵng', N'Hải Châu', N'Thanh Bình', N'Số 3, Phố C', GETDATE(), NULL, NULL, 1),
                                                                                                                                                                                                                                                   (NEWID(), N'customer5', N'123456', N'Phạm Văn D', 'customer5@example.com', '0944444444', 'customer', '1998-04-04', 1, N'Cần Thơ', N'Ninh Kiều', N'An Cư', N'Số 4, Phố D', GETDATE(), NULL, NULL, 1),
                                                                                                                                                                                                                                                   (NEWID(), N'customer6', N'123456', N'Hoàng Thị E', 'customer6@example.com', '0955555556', 'customer', '2000-05-05', 0, N'Hải Phòng', N'Lê Chân', N'An Biên', N'Số 5, Phố E', GETDATE(), NULL, NULL, 1);

update phieu_giam_gia
set so_luong = 100 WHERE id = '5aa57234-8476-451a-a008-1a4128682646';

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


Go

ALTER TABLE don_hang
    ADD trang_thai NVARCHAR(50) NOT NULL DEFAULT 'CHO_XAC_NHAN';

CREATE TABLE lich_su_tim_kiem (
                                  id UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(),
                                  id_nguoi_dung UNIQUEIDENTIFIER NOT NULL,
                                  tu_khoa NVARCHAR(100) NOT NULL,
                                  thoi_gian_tim_kiem DATETIME NOT NULL DEFAULT GETDATE(),
                                  FOREIGN KEY (id_nguoi_dung) REFERENCES nguoi_dung(id)
);


-- ví tài khoản
CREATE TABLE vi_thanh_toan (
                               id UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(),
                               id_nguoi_dung UNIQUEIDENTIFIER NOT NULL,
                               so_du DECIMAL(15, 2) NOT NULL DEFAULT 0.00, -- Số dư ví
                               thoi_gian_tao DATETIME NOT NULL DEFAULT GETDATE(),
                               thoi_gian_cap_nhat DATETIME,
                               trang_thai BIT NOT NULL DEFAULT 1, -- Trạng thái ví (1: hoạt động, 0: khóa)
                               FOREIGN KEY (id_nguoi_dung) REFERENCES nguoi_dung(id)
);
CREATE TABLE lich_su_giao_dich_vi (
                                      id UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(),
                                      id_vi_thanh_toan UNIQUEIDENTIFIER NOT NULL,
                                      id_don_hang UNIQUEIDENTIFIER, -- Liên kết với đơn hàng (nếu có)
                                      loai_giao_dich NVARCHAR(50) NOT NULL CHECK (loai_giao_dich IN (N'Nạp tiền', N'Thanh toán', N'Hoàn tiền', N'Rút tiền')),
                                      so_tien DECIMAL(15, 2) NOT NULL,
                                      thoi_gian_giao_dich DATETIME NOT NULL DEFAULT GETDATE(),
                                      mo_ta NVARCHAR(MAX),
                                      FOREIGN KEY (id_vi_thanh_toan) REFERENCES vi_thanh_toan(id),
                                      FOREIGN KEY (id_don_hang) REFERENCES don_hang(id)
);
ALTER TABLE lich_su_giao_dich_vi ADD created_at DATETIME DEFAULT CURRENT_TIMESTAMP;

INSERT INTO phuong_thuc_thanh_toan (id, ten_phuong_thuc, trang_thai, ngay_tao)
VALUES ('550e8400-e29b-41d4-a716-446655440019', N'Ví', 1, GETDATE());

ALTER TABLE nguoi_dung
    ADD reset_otp NVARCHAR(10) NULL;

ALTER TABLE nguoi_dung
    ADD otp_expiry DATETIME NULL;
select * from nguoi_dung

UPDATE nguoi_dung
SET email = 'datn.acv@gmail.com'
WHERE id = '550E8400-E29B-41D4-A716-446655440014';


DELETE FROM hinh_anh_san_pham
WHERE id IN (
             '550E8400-E29B-41D4-A716-446655440025',
             '550E8400-E29B-41D4-A716-446655440026',
             '550E8400-E29B-41D4-A716-446655440027',
             '550E8400-E29B-41D4-A716-446655440046',
             '550E8400-E29B-41D4-A716-446655440047',
             '550E8400-E29B-41D4-A716-446655440048'
    );


CREATE TABLE yeu_cau_rut_tien (
                                  id UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(),
                                  id_vi_thanh_toan UNIQUEIDENTIFIER NOT NULL,
                                  ma_giao_dich NVARCHAR(20) UNIQUE NOT NULL,
                                  so_tai_khoan VARCHAR(50),
                                  nguoi_thu_huong VARCHAR(100),
                                  ten_ngan_hang VARCHAR(100),
                                  so_tien DECIMAL(15, 2) NOT NULL CHECK (so_tien > 0),
                                  trang_thai NVARCHAR(50) NOT NULL DEFAULT N'Đang chờ',
                                  ghi_chu NVARCHAR(MAX),
                                  thoi_gian_yeu_cau DATETIME NOT NULL DEFAULT GETDATE(),
                                  thoi_gian_xu_ly DATETIME NULL,
                                  FOREIGN KEY (id_vi_thanh_toan) REFERENCES vi_thanh_toan(id)
);

CREATE TABLE dia_chi_nguoi_dung (
                                    id UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(),
                                    id_nguoi_dung UNIQUEIDENTIFIER NOT NULL,
                                    chi_tiet_dia_chi NVARCHAR(255) NOT NULL,
                                    phuong_xa NVARCHAR(100),
                                    quan_huyen NVARCHAR(100),
                                    tinh_thanh_pho NVARCHAR(100),
                                    mac_dinh BIT DEFAULT 0, -- 1 nếu là địa chỉ mặc định
                                    thoi_gian_tao DATETIME DEFAULT GETDATE(),
                                    FOREIGN KEY (id_nguoi_dung) REFERENCES nguoi_dung(id) ON DELETE CASCADE
);

CREATE TABLE phieu_giam_gia_phuong_thuc_thanh_toan (
                                                       id_phieu_giam_gia UNIQUEIDENTIFIER,
                                                       id_phuong_thuc_thanh_toan UNIQUEIDENTIFIER,
                                                       PRIMARY KEY (id_phieu_giam_gia, id_phuong_thuc_thanh_toan),
                                                       FOREIGN KEY (id_phieu_giam_gia) REFERENCES phieu_giam_gia(id),
                                                       FOREIGN KEY (id_phuong_thuc_thanh_toan) REFERENCES phuong_thuc_thanh_toan(id)
);

CREATE TABLE danh_gia (
                          id UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(),
                          id_hoa_don UNIQUEIDENTIFIER NOT NULL,
                          id_chi_tiet_san_pham UNIQUEIDENTIFIER NOT NULL,
                          id_nguoi_dung UNIQUEIDENTIFIER NOT NULL,
                          xep_hang INT NOT NULL CHECK (xep_hang BETWEEN 1 AND 5), -- Xếp hạng từ 1 đến 5 sao
                          noi_dung NVARCHAR(MAX),
                          url_hinh_anh NVARCHAR(MAX), -- Lưu URL của hình ảnh hoặc video
                          thoi_gian_danh_gia DATETIME NOT NULL DEFAULT GETDATE(),
                          trang_thai BIT NOT NULL DEFAULT 1, -- Trạng thái đánh giá (1: hiển thị, 0: ẩn)
                          FOREIGN KEY (id_hoa_don) REFERENCES hoa_don(id),
                          FOREIGN KEY (id_chi_tiet_san_pham) REFERENCES chi_tiet_san_pham(id),
                          FOREIGN KEY (id_nguoi_dung) REFERENCES nguoi_dung(id)
);

CREATE TABLE don_hang_phieu_giam_gia (
                                         id UNIQUEIDENTIFIER PRIMARY KEY,
                                         id_don_hang UNIQUEIDENTIFIER NOT NULL,
                                         id_phieu_giam_gia UNIQUEIDENTIFIER NOT NULL,
                                         loai_giam_gia NVARCHAR(50) NOT NULL,
                                         gia_tri_giam DECIMAL(10, 2) NOT NULL,
                                         thoi_gian_ap_dung DATETIME NOT NULL,
                                         FOREIGN KEY (id_don_hang) REFERENCES don_hang(id),
                                         FOREIGN KEY (id_phieu_giam_gia) REFERENCES phieu_giam_gia(id)
);

CREATE TABLE lich_su_doi_san_pham (
                                      id UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(),
                                      id_chi_tiet_don_hang UNIQUEIDENTIFIER NOT NULL,
                                      id_hoa_don UNIQUEIDENTIFIER NOT NULL,
                                      id_chi_tiet_san_pham_thay_the UNIQUEIDENTIFIER NOT NULL,
                                      so_luong INT NOT NULL CHECK (so_luong > 0),
                                      tong_tien_hoan DECIMAL(10,2) NOT NULL,
                                      ly_do_doi_hang NVARCHAR(MAX),
                                      thoi_gian_doi DATETIME NOT NULL DEFAULT GETDATE(),
                                      trang_thai NVARCHAR(50) NOT NULL DEFAULT N'Chờ xử lý',
                                      FOREIGN KEY (id_chi_tiet_don_hang) REFERENCES chi_tiet_don_hang(id),
                                      FOREIGN KEY (id_hoa_don) REFERENCES hoa_don(id),
                                      FOREIGN KEY (id_chi_tiet_san_pham_thay_the) REFERENCES chi_tiet_san_pham(id)
);

ALTER TABLE chi_tiet_don_hang
    ADD trang_thai_doi_san_pham BIT NOT NULL DEFAULT 0,
    ly_do_doi_hang NVARCHAR(MAX);

ALTER TABLE lich_su_doi_san_pham
    ADD chenh_lech_gia DECIMAL(10,2) NULL;

ALTER TABLE dia_chi_nguoi_dung
    ADD nguoi_nhan NVARCHAR(100) NULL,
    so_dien_thoai_nguoi_nhan NVARCHAR(20) NULL;

ALTER TABLE don_hang
    ADD id_dia_chi UNIQUEIDENTIFIER,
FOREIGN KEY (id_dia_chi) REFERENCES dia_chi_nguoi_dung(id);

ALTER TABLE phieu_giam_gia
ALTER COLUMN ngay_bat_dau DATETIME;

ALTER TABLE phieu_giam_gia
ALTER COLUMN ngay_ket_thuc DATETIME;

ALTER TABLE chien_dich_giam_gia
ALTER COLUMN ngay_bat_dau DATETIME2(0) NOT NULL;

ALTER TABLE chien_dich_giam_gia
ALTER COLUMN ngay_ket_thuc DATETIME2(0) NOT NULL;

ALTER TABLE thong_bao_nhom
ALTER COLUMN id_don_hang UNIQUEIDENTIFIER NULL;

ALTER TABLE hoa_don
    ADD nhan_vien_id UNIQUEIDENTIFIER NULL,
FOREIGN KEY (nhan_vien_id) REFERENCES nguoi_dung(id);

ALTER TABLE dbo.phieu_giam_gia
    ADD pham_vi_ap_dung NVARCHAR(20) NULL;
GO

UPDATE dbo.phieu_giam_gia
SET pham_vi_ap_dung = 'ORDER'
WHERE pham_vi_ap_dung IS NULL;
GO

ALTER TABLE dbo.phieu_giam_gia
    ADD CONSTRAINT DF_phieu_giam_gia_pham_vi_ap_dung DEFAULT N'ORDER' FOR pham_vi_ap_dung;

ALTER TABLE dbo.phieu_giam_gia
ALTER COLUMN pham_vi_ap_dung NVARCHAR(20) NOT NULL;

ALTER TABLE don_hang ALTER COLUMN phi_van_chuyen     DECIMAL(18,2) NULL;
ALTER TABLE don_hang ALTER COLUMN so_tien_khach_dua  DECIMAL(18,2) NULL;
ALTER TABLE don_hang ALTER COLUMN tien_giam          DECIMAL(18,2) NULL;
ALTER TABLE don_hang ALTER COLUMN tong_tien          DECIMAL(18,2) NOT NULL;

ALTER TABLE chi_tiet_don_hang ALTER COLUMN gia        DECIMAL(18,2) NOT NULL;
ALTER TABLE chi_tiet_don_hang ALTER COLUMN thanh_tien DECIMAL(18,2) NOT NULL;

ALTER TABLE lich_su_doi_san_pham
    ADD id_phuong_thuc_thanh_toan UNIQUEIDENTIFIER NULL
    CONSTRAINT FK_lsdoi_pttt FOREIGN KEY (id_phuong_thuc_thanh_toan)
        REFERENCES phuong_thuc_thanh_toan(id);

ALTER TABLE lich_su_doi_san_pham
    ADD da_thanh_toan_chenh_lech BIT NOT NULL DEFAULT 0;