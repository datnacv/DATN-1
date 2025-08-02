document.addEventListener('DOMContentLoaded', function () {
    console.log('DOM loaded, initializing notification system...');

    if (typeof bootstrap === 'undefined') {
        console.error('Bootstrap JavaScript not loaded. Ensure Bootstrap 5.1.3 is included.');
        alert('Lỗi hệ thống: Không thể tải thư viện giao diện. Vui lòng thử lại sau.');
        return;
    }

    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
    if (!csrfToken || !csrfHeader) {
        console.warn('CSRF token or header not found. POST requests may fail.');
    }

    // Hàm debounce để ngăn gọi API nhiều lần
    function debounce(func, wait) {
        let timeout;
        return function executedFunction(...args) {
            const later = () => {
                clearTimeout(timeout);
                func(...args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    }

    // Xử lý chuông thông báo
    const trigger = document.getElementById('notificationDropdown');
    if (trigger) {
        const dropdown = trigger.closest('.dropdown');
        if (dropdown) {
            trigger.addEventListener('click', debounce(function () {
                console.log('Notification bell clicked, triggering loadNotifications...');
                loadNotifications();
            }, 300));
        } else {
            console.warn('Dropdown container not found for notificationDropdown');
        }
    } else {
        console.warn('Notification dropdown trigger not found');
    }

    // Tải thông báo cho bảng nếu có
    const tableBody = document.getElementById('notificationTableBody');
    if (tableBody) {
        loadNotificationsForTable();
    }

    // Xử lý liên kết "Xem tất cả"
    const viewAllLinks = document.querySelectorAll('a[data-bs-target="#notificationModal"]');
    viewAllLinks.forEach(link => {
        link.addEventListener('click', function (e) {
            e.preventDefault(); // Ngăn hành vi mặc định của Bootstrap
            console.log('View all notifications link clicked');
            const modal = document.getElementById('notificationModal');
            if (!modal) {
                console.error('Modal element not found for ID: notificationModal');
                alert('Lỗi hệ thống: Không tìm thấy cửa sổ thông báo.');
                return;
            }
            try {
                console.log('Attempting to initialize and show modal');
                const modalInstance = new bootstrap.Modal(modal);
                modalInstance.show();
                console.log('Modal opened successfully');
                // Tải thông báo khi modal mở
                modal.addEventListener('shown.bs.modal', function () {
                    console.log('Modal is shown, loading notifications...');
                    loadNotificationsForModal();
                }, { once: true }); // Chỉ gọi một lần khi modal mở
            } catch (e) {
                console.error('Error showing modal:', e);
                alert('Lỗi hệ thống: Không thể hiển thị cửa sổ thông báo. Vui lòng thử lại sau.');
            }
        });
    });

    function loadNotifications() {
        const loadingState = document.getElementById('loadingState');
        const emptyState = document.getElementById('emptyState');
        const notificationContainer = document.getElementById('notificationContainer');
        const anchor = document.getElementById('notificationInsertAnchor');

        if (!notificationContainer || !anchor) {
            console.error('Notification container or anchor not found');
            return;
        }

        if (loadingState) loadingState.classList.remove('d-none');
        if (emptyState) emptyState.classList.add('d-none');

        const existingNotifications = notificationContainer.querySelectorAll('.notification-item:not(#loadingState):not(#emptyState)') || [];
        existingNotifications.forEach(item => item.remove());

        fetch('/acvstore/thong-bao/load?_=' + new Date().getTime())
            .then(response => {
                console.log('Response status:', response.status);
                if (!response.ok) {
                    throw new Error(`Lỗi kết nối! Mã trạng thái: ${response.status}`);
                }
                return response.json();
            })
            .then(data => {
                console.log('Data received:', data);
                console.log('Số thông báo nhận được:', data.notifications ? data.notifications.length : 0);
                if (loadingState) loadingState.classList.add('d-none');
                if (data.notifications && Array.isArray(data.notifications) && data.notifications.length > 0) {
                    renderNotifications(data.notifications.slice(0, 5));
                    updateUnreadCount(data.unreadCount || 0);
                } else {
                    if (emptyState) emptyState.classList.remove('d-none');
                }
                checkEmptyState();
            })
            .catch(error => {
                console.error('Lỗi tải thông báo:', error);
                if (loadingState) loadingState.classList.add('d-none');
                const errorItem = document.createElement('div');
                errorItem.className = 'alert alert-danger m-3';
                errorItem.innerHTML = 'Không thể tải thông báo. Vui lòng thử lại sau.';
                anchor.before(errorItem);
            });
    }

    function loadNotificationsForTable() {
        const loadingState = document.getElementById('tableLoadingState');
        const emptyState = document.getElementById('tableEmptyState');
        const tableBody = document.getElementById('notificationTableBody');

        if (!tableBody) {
            console.error('Table body not found');
            return;
        }

        if (loadingState) loadingState.classList.remove('d-none');
        if (emptyState) emptyState.classList.add('d-none');

        const existingNotifications = tableBody.querySelectorAll('tr:not(#tableLoadingState):not(#tableEmptyState)') || [];
        existingNotifications.forEach(item => item.remove());

        fetch('/acvstore/thong-bao/load?_=' + new Date().getTime())
            .then(response => {
                console.log('Response status:', response.status);
                if (!response.ok) {
                    throw new Error(`Lỗi kết nối! Mã trạng thái: ${response.status}`);
                }
                return response.json();
            })
            .then(data => {
                console.log('Số thông báo nhận được (table):', data.notifications ? data.notifications.length : 0);
                if (loadingState) loadingState.classList.add('d-none');
                if (data.notifications && Array.isArray(data.notifications) && data.notifications.length > 0) {
                    renderNotificationsForTable(data.notifications.slice(0, 5), false);
                    updateUnreadCount(data.unreadCount || 0);
                } else {
                    if (emptyState) emptyState.classList.remove('d-none');
                }
                checkEmptyState();
            })
            .catch(error => {
                console.error('Lỗi tải thông báo cho bảng:', error);
                if (loadingState) loadingState.classList.add('d-none');
                const errorRow = document.createElement('tr');
                errorRow.innerHTML = `
                    <td colspan="5" class="text-center">
                        <div class="alert alert-danger m-3">Không thể tải thông báo. Vui lòng thử lại sau.</div>
                    </td>
                `;
                tableBody.appendChild(errorRow);
            });
    }

    function loadNotificationsForModal(page = 0, size = 5) {
        const loadingState = document.getElementById('modalTableLoadingState');
        const emptyState = document.getElementById('modalTableEmptyState');
        const tableBody = document.getElementById('modalNotificationTableBody');

        if (!tableBody) {
            console.error('Modal table body not found');
            return;
        }

        if (loadingState) loadingState.classList.remove('d-none');
        if (emptyState) emptyState.classList.add('d-none');

        const existingNotifications = tableBody.querySelectorAll('tr:not(#modalTableLoadingState):not(#modalTableEmptyState)') || [];
        existingNotifications.forEach(item => item.remove());

        console.log(`Loading notifications for modal with page=${page}, size=${size}`);
        fetch(`/acvstore/thong-bao/xem?page=${page}&size=${size}&_=${new Date().getTime()}`)
            .then(response => {
                console.log('Response status:', response.status);
                if (!response.ok) {
                    throw new Error(`Lỗi kết nối! Mã trạng thái: ${response.status}`);
                }
                return response.json();
            })
            .then(data => {
                console.log('Số thông báo nhận được (modal):', data.notifications ? data.notifications.length : 0);
                if (loadingState) loadingState.classList.add('d-none');
                if (data.notifications && Array.isArray(data.notifications) && data.notifications.length > 0) {
                    renderNotificationsForTable(data.notifications, true);
                    updateUnreadCount(data.unreadCount || 0);
                } else {
                    if (emptyState) emptyState.classList.remove('d-none');
                }
                checkEmptyState();
            })
            .catch(error => {
                console.error('Lỗi tải thông báo cho modal:', error);
                if (loadingState) loadingState.classList.add('d-none');
                const errorRow = document.createElement('tr');
                errorRow.innerHTML = `
                    <td colspan="5" class="text-center">
                        <div class="alert alert-danger m-3">Không thể tải thông báo. Vui lòng thử lại sau.</div>
                    </td>
                `;
                tableBody.appendChild(errorRow);
            });
    }

    function renderNotifications(notifications) {
        const anchor = document.getElementById('notificationInsertAnchor');
        const emptyState = document.getElementById('emptyState');

        if (!anchor) {
            console.error('Notification insert anchor not found');
            return;
        }

        if (!notifications || !Array.isArray(notifications) || notifications.length === 0) {
            if (emptyState) emptyState.classList.remove('d-none');
            return;
        }

        notifications.slice(0, 5).forEach((notification, index) => {
            const timeAgo = formatTimeAgo(notification.thoiGian);
            const icon = '<i class="fas fa-info-circle text-primary"></i>';
            const tieuDe = notification.tieuDe || 'Không có tiêu đề';
            const noiDung = notification.noiDung || 'Không có nội dung';
            const item = document.createElement('li');
            item.className = `notification-item px-3 py-2 ${notification.daXem ? '' : 'notification-unread'}`;
            item.style.cursor = 'pointer';
            item.onclick = () => markAsRead(notification.idThongBao, item);
            item.innerHTML = `
                <div class="d-flex align-items-start">
                    <div class="me-2">${icon}</div>
                    <div class="flex-grow-1">
                        <div class="fw-semibold mb-1">${escapeHtml(tieuDe)}</div>
                        <div class="text-muted notification-content">${escapeHtml(noiDung)}</div>
                        <div class="text-end notification-time mt-1">${timeAgo}</div>
                    </div>
                    ${notification.daXem ? '' : '<span class="badge bg-primary ms-2">Mới</span>'}
                </div>
            `;
            anchor.before(item);
        });
    }

    function renderNotificationsForTable(notifications, isModal = false) {
        const tableBody = isModal ? document.getElementById('modalNotificationTableBody') : document.getElementById('notificationTableBody');
        const emptyState = isModal ? document.getElementById('modalTableEmptyState') : document.getElementById('tableEmptyState');

        if (!tableBody) {
            console.error('Table body not found');
            return;
        }

        if (!notifications || !Array.isArray(notifications) || notifications.length === 0) {
            if (emptyState) emptyState.classList.remove('d-none');
            return;
        }

        notifications.forEach((notification, index) => {
            const timeAgo = formatTimeAgo(notification.thoiGian);
            const tieuDe = notification.tieuDe || 'Không có tiêu đề';
            const noiDung = notification.noiDung || 'Không có nội dung';
            const row = document.createElement('tr');
            row.className = notification.daXem ? '' : 'notification-unread';
            row.style.cursor = 'pointer';
            row.onclick = () => markAsRead(notification.idThongBao, row);
            row.innerHTML = `
                <td>${index + 1}</td>
                <td>${escapeHtml(tieuDe)}</td>
                <td>${escapeHtml(noiDung)}</td>
                <td>${timeAgo}</td>
                <td>${notification.daXem ? 'Đã đọc' : '<span class="badge bg-primary">Mới</span>'}</td>
            `;
            tableBody.appendChild(row);
        });
    }

    function escapeHtml(text) {
        if (!text) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    function formatTimeAgo(thoiGian) {
        if (!thoiGian) {
            console.warn('thoiGian is null or undefined');
            return 'Không rõ thời gian';
        }
        try {
            let dateObj;
            if (typeof thoiGian === 'string') {
                if (thoiGian.includes('/')) {
                    const parts = thoiGian.split(/[/ :]/);
                    dateObj = new Date(`${parts[2]}-${parts[1].padStart(2, '0')}-${parts[0].padStart(2, '0')}T${parts[3].padStart(2, '0')}:${parts[4].padStart(2, '0')}:00`);
                } else {
                    dateObj = new Date(thoiGian);
                }
            } else if (typeof thoiGian === 'number') {
                dateObj = new Date(thoiGian);
            } else {
                console.warn('Invalid thoiGian format:', thoiGian);
                return 'Không rõ thời gian';
            }

            if (!dateObj || isNaN(dateObj.getTime())) {
                console.warn('Invalid date object for thoiGian:', thoiGian);
                return 'Không rõ thời gian';
            }

            const now = new Date();
            const seconds = Math.floor((now - dateObj) / 1000);
            if (seconds < 60) return 'Vừa xong';
            const minutes = Math.floor(seconds / 60);
            if (minutes < 60) return `${minutes} phút trước`;
            const hours = Math.floor(minutes / 60);
            if (hours < 24) return `${hours} giờ trước`;
            const days = Math.floor(hours / 24);
            if (days < 7) return `${days} ngày trước`;
            return dateObj.toLocaleDateString('vi-VN', {
                day: '2-digit',
                month: '2-digit',
                year: 'numeric',
                hour: '2-digit',
                minute: '2-digit'
            });
        } catch (e) {
            console.error('Lỗi định dạng thời gian:', e, 'thoiGian:', thoiGian);
            return 'Không rõ thời gian';
        }
    }

    function markAsRead(notificationId, element) {
        if (!csrfToken || !csrfHeader) {
            alert('Lỗi hệ thống: Thiếu thông tin bảo mật. Vui lòng thử lại sau.');
            return;
        }
        fetch('/acvstore/thong-bao/danh-dau-da-xem', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
                [csrfHeader]: csrfToken
            },
            body: `idThongBao=${notificationId}`
        })
            .then(response => {
                if (response.ok) {
                    element.classList.remove('notification-unread');
                    element.style.cursor = 'default';
                    const indicator = element.querySelector('.badge');
                    if (indicator) indicator.remove();
                    if (element.tagName === 'TR') {
                        const statusCell = element.querySelector('td:last-child');
                        if (statusCell) statusCell.textContent = 'Đã đọc';
                    }
                    loadNotifications();
                    loadNotificationsForTable();
                    loadNotificationsForModal();
                } else {
                    throw new Error(`Lỗi kết nối! Mã trạng thái: ${response.status}`);
                }
            })
            .catch(error => {
                console.error('Lỗi đánh dấu thông báo đã đọc:', error);
                alert('Không thể đánh dấu thông báo đã đọc. Vui lòng thử lại sau.');
            });
    }

    function markAllAsRead() {
        if (!csrfToken || !csrfHeader) {
            alert('Lỗi hệ thống: Thiếu thông tin bảo mật. Vui lòng thử lại sau.');
            return;
        }
        fetch('/acvstore/thong-bao/danh-dau-tat-ca', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
                [csrfHeader]: csrfToken
            }
        })
            .then(response => {
                if (response.ok) {
                    const unreadItems = document.querySelectorAll('.notification-unread');
                    unreadItems.forEach(item => {
                        item.classList.remove('notification-unread');
                        item.style.cursor = 'default';
                        const indicator = item.querySelector('.badge');
                        if (indicator) indicator.remove();
                        if (item.tagName === 'TR') {
                            const statusCell = item.querySelector('td:last-child');
                            if (statusCell) statusCell.textContent = 'Đã đọc';
                        }
                    });
                    updateUnreadCount(0);
                    const markAllButtons = document.querySelectorAll('[onclick="markAllAsRead()"]');
                    markAllButtons.forEach(btn => btn.style.display = 'none');
                    if (document.getElementById('modalNotificationTableBody')) loadNotificationsForModal();
                    if (document.getElementById('notificationTableBody')) loadNotificationsForTable();
                } else {
                    throw new Error(`Lỗi kết nối! Mã trạng thái: ${response.status}`);
                }
            })
            .catch(error => {
                console.error('Lỗi đánh dấu tất cả thông báo đã đọc:', error);
                alert('Không thể đánh dấu tất cả thông báo đã đọc. Vui lòng thử lại sau.');
            });
    }

    function updateUnreadCount(count) {
        const badge = document.querySelector('.notification-badge span');
        const badgeContainer = document.querySelector('.notification-badge');
        if (count > 0) {
            if (badge) badge.textContent = count;
            if (badgeContainer) badgeContainer.style.display = 'inline';
        } else {
            if (badgeContainer) badgeContainer.style.display = 'none';
        }
    }

    function checkEmptyState() {
        const modalTableBody = document.getElementById('modalNotificationTableBody');
        const tableBody = document.getElementById('notificationTableBody');
        const notificationContainer = document.getElementById('notificationContainer');

        if (modalTableBody) {
            const items = modalTableBody.querySelectorAll('tr:not(#modalTableLoadingState):not(#modalTableEmptyState)');
            const emptyState = document.getElementById('modalTableEmptyState');
            if (items.length === 0 && emptyState) {
                emptyState.classList.remove('d-none');
            }
        }

        if (tableBody) {
            const items = tableBody.querySelectorAll('tr:not(#tableLoadingState):not(#tableEmptyState)');
            const emptyState = document.getElementById('tableEmptyState');
            if (items.length === 0 && emptyState) {
                emptyState.classList.remove('d-none');
            }
        }

        if (notificationContainer) {
            const items = notificationContainer.querySelectorAll('.notification-item:not(#loadingState):not(#emptyState)');
            const emptyState = document.getElementById('emptyState');
            if (items.length === 0 && emptyState) {
                emptyState.classList.remove('d-none');
            }
        }
    }
});