/**
 * Dashboard Module
 * Handles dashboard functionality and API calls
 */

class DashboardService {
    constructor() {
        this.apiBaseUrl = '/api';
    }

    /**
     * Make authenticated API request
     */
    async makeRequest(url, options = {}) {
        const headers = {
            'Content-Type': 'application/json',
            ...options.headers
        };

        const token = window.getStoredAuthToken
            ? window.getStoredAuthToken()
            : (localStorage.getItem('auth_token') || localStorage.getItem('authToken') || localStorage.getItem('token'));
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }

        const response = await fetch(url, {
            ...options,
            headers,
            credentials: 'same-origin'
        });

        if (!response.ok) {
            if (response.status === 401) {
                if (window.clearStoredAuthTokens) {
                    window.clearStoredAuthTokens();
                } else {
                    ['token', 'auth_token', 'authToken', 'refresh_token'].forEach(key => localStorage.removeItem(key));
                }
                window.location.href = '/api/pages/login';
            }
            const error = new Error(`HTTP error! status: ${response.status}`);
            error.status = response.status;
            throw error;
        }

        return await response.json();
    }

    /**
     * Get dashboard statistics
     */
    async getStatistics() {
        try {
            const response = await this.makeRequest(`${this.apiBaseUrl}/dashboard/statistics`);
            return response;
        } catch (error) {
            console.error('Error fetching statistics:', error);
            return null;
        }
    }

    /**
     * Get recent orders
     */
    async getRecentOrders(limit = 5) {
        try {
            const response = await this.makeRequest(
                `${this.apiBaseUrl}/orders?limit=${limit}&sort=orderDate,desc`
            );
            return response;
        } catch (error) {
            console.error('Error fetching recent orders:', error);
            return null;
        }
    }

    /**
     * Get current user info
     */
    async getCurrentUser() {
        try {
            const response = await this.makeRequest(`${this.apiBaseUrl}/users/me`);
            return response;
        } catch (error) {
            console.error('Error fetching user info:', error);
            return null;
        }
    }
}

const dashboardService = new DashboardService();

// Page content cache
const pageCache = {};
const loadedPageScripts = new Set(
    Array.from(document.querySelectorAll('script[src]')).map(s => s.getAttribute('src'))
);
const THEME_STORAGE_KEY = 'metric-dashboard-theme';
let currentDashboardPage = 'dashboard';
let latestDashboardStats = null;
let statusChartInstance = null;
const sparklineInstances = {}; 

function applyTheme(theme) {
    const normalized = theme === 'dark' ? 'dark' : 'light';
    document.body.setAttribute('data-theme', normalized);

    const toggle = document.getElementById('themeToggle');
    if (toggle) {
        toggle.innerHTML = normalized === 'dark'
            ? '<i class="fas fa-sun"></i>'
            : '<i class="fas fa-moon"></i>';
        toggle.setAttribute('title', normalized === 'dark' ? 'Switch to light mode' : 'Switch to dark mode');
    }
}

function initializeTheme() {
    const savedTheme = localStorage.getItem(THEME_STORAGE_KEY) || 'light';
    applyTheme(savedTheme);
}

function toggleTheme() {
    const currentTheme = document.body.getAttribute('data-theme') === 'dark' ? 'dark' : 'light';
    const nextTheme = currentTheme === 'dark' ? 'light' : 'dark';
    localStorage.setItem(THEME_STORAGE_KEY, nextTheme);
    applyTheme(nextTheme);
    showAppToast(nextTheme === 'dark' ? 'Dark mode enabled' : 'Light mode enabled', 'info');
}

function showAppToast(message, type = 'primary') {
    const container = document.getElementById('appToastContainer');
    if (!container || !window.bootstrap) return;

    const toast = document.createElement('div');
    toast.className = `toast toast-modern align-items-center border-0 text-bg-${type}`;
    toast.setAttribute('role', 'alert');
    toast.setAttribute('aria-live', 'assertive');
    toast.setAttribute('aria-atomic', 'true');
    toast.innerHTML = `
        <div class="d-flex">
            <div class="toast-body"></div>
            <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
        </div>
    `;
    toast.querySelector('.toast-body').textContent = String(message ?? '');
    container.appendChild(toast);
    const instance = new bootstrap.Toast(toast, { delay: 2200 });
    toast.addEventListener('hidden.bs.toast', () => toast.remove());
    instance.show();
}

window.showAppToast = showAppToast;

/**
 * Initialize Dashboard
 */
$(document).ready(function () {
    initializeTheme();
    initializeDashboard();
    loadDashboardData();
    setupEventListeners();
});

/**
 * Initialize dashboard UI
 */
function initializeDashboard() {
    // Toggle sidebar - ONLY toggle, don't navigate
    $('#toggleSidebar').click(function (e) {
        e.preventDefault();
        e.stopPropagation();
        $('.sidebar').toggleClass('active');
    });

    const themeToggle = document.getElementById('themeToggle');
    if (themeToggle && !themeToggle.dataset.bound) {
        themeToggle.addEventListener('click', toggleTheme);
        themeToggle.dataset.bound = 'true';
    }
}

/**
 * Navigate to a page dynamically
 */
async function navigateTo(page, event) {
    if (event) {
        event.preventDefault();
        event.stopPropagation();
    }

    // Don't reload if already on this page
    if (currentDashboardPage === page) {
        return;
    }

    try {
        // Show loading state
        showLoadingState();

        // Get page content
        const content = await loadPageContent(page);

        // Update content area
        await updateContentArea(content, page);

        // Update active menu item
        updateActiveMenuItem(page);

        // Close sidebar on mobile
        if ($(window).width() < 768) {
            $('.sidebar').removeClass('active');
        }

        // Load page-specific scripts
        loadPageScripts(page);

        currentDashboardPage = page;
    } catch (error) {
        console.error('Error navigating to page:', error);
        showErrorState('Failed to load page');
    }
}

/**
 * Load page content via AJAX
 */
async function loadPageContent(page) {
    try {
        const response = await fetch(`/api/pages/${page}`, {
            credentials: 'same-origin',
            cache: 'no-store'
        });

        if (response.redirected && response.url.includes('/pages/login')) {
            window.location.href = '/api/pages/login';
            throw new Error('Session expired. Please login again.');
        }

        if (!response.ok) {
            throw new Error(`Failed to load page: ${response.status}`);
        }

        const html = await response.text();
        const tempDiv = document.createElement('div');
        tempDiv.innerHTML = html;

        const body = tempDiv.querySelector('body');
        const inlineStyles = Array.from(tempDiv.querySelectorAll('head style'))
            .map(style => style.outerHTML)
            .join('\n');
        let contentHTML = '';

        if (body) {
            const bodyClone = body.cloneNode(true);
            bodyClone.querySelectorAll('.sidebar, .top-header').forEach(node => node.remove());

            const content = bodyClone.querySelector('.dashboard-content') ||
                bodyClone.querySelector('#mainContent') ||
                bodyClone.querySelector('.main-content') ||
                bodyClone.querySelector('main') ||
                bodyClone.querySelector('.container-fluid') ||
                bodyClone;

            contentHTML = content ? (content.outerHTML || content.innerHTML) : bodyClone.innerHTML;

            if (content && content !== bodyClone) {
                const extraNodes = Array.from(bodyClone.children).filter(node =>
                    node !== content &&
                    node.nodeType === Node.ELEMENT_NODE &&
                    !node.matches?.('.sidebar, .top-header') &&
                    (node.matches?.('.modal, .offcanvas, .toast, script, style') ||
                     node.querySelector?.('.modal, .offcanvas, .toast, script, style'))
                );

                if (extraNodes.length > 0) {
                    contentHTML += '\n' + extraNodes.map(node => node.outerHTML).join('\n');
                }
            }
        } else {
            const content = tempDiv.querySelector('.dashboard-content') ||
                tempDiv.querySelector('#mainContent') ||
                tempDiv.querySelector('.main-content') ||
                tempDiv.querySelector('main') ||
                tempDiv.querySelector('.container-fluid') ||
                tempDiv;

            contentHTML = content ? (content.outerHTML || content.innerHTML) : html;

            if (content && content !== tempDiv) {
                const extraNodes = Array.from(tempDiv.children).filter(node =>
                    node !== content &&
                    node.nodeType === Node.ELEMENT_NODE &&
                    (node.matches?.('.modal, .offcanvas, .toast, script, style') ||
                     node.querySelector?.('.modal, .offcanvas, .toast, script, style'))
                );

                if (extraNodes.length > 0) {
                    contentHTML += '\n' + extraNodes.map(node => node.outerHTML).join('\n');
                }
            }
        }

        if (inlineStyles) {
            contentHTML = inlineStyles + '\n' + contentHTML;
        }

        pageCache[page] = contentHTML;
        return contentHTML;
    } catch (error) {
        console.error('Error loading page content:', error);
        throw error;
    }
}

/**
 * Update the content area with new content
 */
async function updateContentArea(contentData, page) {
    const wrapper = document.getElementById('contentWrapper');
    if (wrapper) {
        const titleMap = {
            'dashboard': 'Dashboard',
            'customers': 'Customers',
            'orders': 'Orders',
            'measurements': 'Measurements',
            'payments': 'Payments',
            'reports': 'Reports',
            'settings': 'Settings',
            'leads': 'Leads',
            'crm': 'CRM',
            'quotation': 'Quotation',
            'production': 'Production',
            'qc': 'QC & Delivery',
            'aftersales': 'After-sales',
            'finance': 'Finance'
        };

        const headerTitle = document.querySelector('.top-header h1');
        if (headerTitle) {
            headerTitle.textContent = titleMap[page] || 'Page';
        }

        const html = typeof contentData === 'string' ? contentData : String(contentData || '');
        const tempDiv = document.createElement('div');
        tempDiv.innerHTML = html;
        tempDiv.querySelectorAll('.sidebar, .top-header').forEach(node => node.remove());

        const scriptElements = Array.from(tempDiv.querySelectorAll('script'));
        scriptElements.forEach(script => script.remove());

        wrapper.innerHTML = tempDiv.innerHTML;

        for (const oldScript of scriptElements) {
            try {
                const src = oldScript.getAttribute('src');
                if (src) {
                    await ensureExternalScript(src);
                } else {
                    const scriptContent = oldScript.textContent || '';
                    if (scriptContent.trim()) {
                        const inlineScript = document.createElement('script');
                        inlineScript.textContent = scriptContent;
                        document.body.appendChild(inlineScript);
                        setTimeout(() => inlineScript.remove(), 100);
                    }
                }
            } catch (error) {
                console.error('Error executing script:', error);
            }
        }

        window.scrollTo(0, 0);
    }
}

function ensureExternalScript(src) {
    return new Promise((resolve, reject) => {
        if (loadedPageScripts.has(src)) {
            resolve();
            return;
        }

        const script = document.createElement('script');
        script.src = src;
        script.onload = () => {
            loadedPageScripts.add(src);
            resolve();
        };
        script.onerror = reject;
        document.body.appendChild(script);
    });
}

/**
 * Update active menu item
 */
function updateActiveMenuItem(page) {
    document.querySelectorAll('.sidebar-menu .nav-link').forEach(link => {
        link.classList.remove('active');
    });
    
    const activeLink = document.querySelector(`.sidebar-menu [data-page="${page}"]`);
    if (activeLink) {
        activeLink.classList.add('active');
    }
}

/**
 * Safely close a Bootstrap modal by ID
 * @param {string} modalId - ID of the modal to close
 */
function closeModal(modalId) {
    const modalEl = document.getElementById(modalId);
    if (!modalEl) return;
    
    try {
        // Try Bootstrap method
        const modalInstance = bootstrap.Modal.getInstance(modalEl);
        if (modalInstance) {
            modalInstance.hide();
            return;
        }
    } catch (e) {
        console.warn('Bootstrap Modal.getInstance failed:', e);
    }
    
    // Fallback: Manual close
    modalEl.classList.remove('show');
    modalEl.setAttribute('aria-hidden', 'true');
    modalEl.style.display = 'none';
    document.body.classList.remove('modal-open');
    
    // Remove all backdrop elements
    const backdrops = document.querySelectorAll('.modal-backdrop');
    backdrops.forEach(bd => bd.remove());
    
    // Remove any padding added by modal
    if (document.body.style.paddingRight) {
        document.body.style.paddingRight = '';
    }
}

/**
 * Load page-specific scripts
 */
function loadPageScripts(page) {
    // Trigger custom event for page-specific initialization
    // Page modules (customers.js, settings.js, etc.) will listen for this event
    const event = new CustomEvent('pageLoaded', { detail: { page: page } });
    document.dispatchEvent(event);

    if (page === 'dashboard') {
        loadDashboardData();
    }
}

/**
 * Show loading state
 */
function showLoadingState() {
    const wrapper = document.getElementById('contentWrapper');
    if (wrapper) {
        wrapper.innerHTML = `
            <div class="loading-shell">
                <div class="loading-skeleton-grid">
                    <div class="skeleton-card"></div>
                    <div class="skeleton-card"></div>
                    <div class="skeleton-card"></div>
                    <div class="skeleton-card"></div>
                </div>
                <div class="card p-4 mt-3">
                    <div class="skeleton-line w-25 mb-3"></div>
                    <div class="skeleton-line w-100"></div>
                    <div class="skeleton-line w-75"></div>
                    <div class="skeleton-line w-50"></div>
                </div>
            </div>
        `;
    }
}

/**
 * Show error state
 */
function showErrorState(message) {
    const wrapper = document.getElementById('contentWrapper');
    if (wrapper) {
        wrapper.innerHTML = `
            <div class="alert alert-danger" role="alert">
                <i class="fas fa-exclamation-circle"></i> ${message}
            </div>
        `;
    }
    showAppToast(message, 'danger');
}

/**
 * Handle logout
 */
async function handleLogout(event) {
    event.preventDefault();
    event.stopPropagation();
    
    if (!confirm('Are you sure you want to logout?')) {
        return;
    }

    try {
        const token = window.getStoredAuthToken
            ? window.getStoredAuthToken()
            : (localStorage.getItem('auth_token') || localStorage.getItem('authToken') || localStorage.getItem('token'));
        const headers = token ? { Authorization: `Bearer ${token}` } : {};

        await fetch('/api/auth/logout', {
            method: 'POST',
            headers,
            credentials: 'same-origin'
        });
    } catch (error) {
        console.warn('Logout request failed, clearing local session anyway.', error);
    } finally {
        if (window.clearStoredAuthTokens) {
            window.clearStoredAuthTokens();
        } else {
            ['token', 'auth_token', 'authToken', 'refresh_token'].forEach(key => localStorage.removeItem(key));
        }
        window.location.href = '/api/pages/login';
    }
}

/**
 * Load dashboard data
 */
async function loadDashboardData() {
    try {
        // Load user info
        const userResponse = await dashboardService.getCurrentUser();
        if (userResponse && userResponse.data) {
            _cachedProfile = userResponse.data;
            updateHeaderAvatar(userResponse.data);
        }

        // Load statistics
        const statsResponse = await dashboardService.getStatistics();
        if (statsResponse && statsResponse.data) {
            updateStatistics(statsResponse.data);
        }

        // Load recent orders
        const ordersResponse = await dashboardService.getRecentOrders();
        if (ordersResponse && ordersResponse.data) {
            updateRecentOrders(ordersResponse.data);
        }

        // Initialize charts
        initializeCharts();
    } catch (error) {
        console.error('Error loading dashboard data:', error);
    }
}

/**
 * Update statistics cards
 */
function animateValue(elementId, value, format = 'number') {
    const el = document.getElementById(elementId);
    if (!el) return;

    const finalValue = Number(value || 0);
    el.dataset.value = String(finalValue);
    el.dataset.format = format;

    const duration = 800;
    const start = 0;
    const startTime = performance.now();

    function render(val) {
        if (format === 'currency') {
            el.textContent = '$' + Number(val).toFixed(2);
        } else {
            el.textContent = Math.round(val).toLocaleString('en-US');
        }
    }

    function tick(now) {
        const progress = Math.min((now - startTime) / duration, 1);
        const eased = 1 - Math.pow(1 - progress, 3);
        const current = start + (finalValue - start) * eased;
        render(current);
        if (progress < 1) {
            requestAnimationFrame(tick);
        } else {
            render(finalValue);
        }
    }

    requestAnimationFrame(tick);
}

function updateHeroHighlights(stats) {
    const activeOrders = Number(stats.activeOrders || 0);
    const completedOrders = Number(stats.completedOrders || 0);
    const customers = Number(stats.totalCustomers || 0);

    const heroValue = document.getElementById('heroOrdersValue');
    const heroSub = document.getElementById('heroOrdersSub');

    if (heroValue) heroValue.textContent = activeOrders.toLocaleString('en-US');
    if (heroSub) heroSub.textContent = `${completedOrders.toLocaleString('en-US')} đơn đã hoàn tất • ${customers.toLocaleString('en-US')} khách hàng đang theo dõi`;
}

function updateStatistics(stats) {
    latestDashboardStats = stats;
    animateValue('totalCustomers', stats.totalCustomers || 0, 'number');
    animateValue('activeOrders', stats.activeOrders || 0, 'number');
    animateValue('pendingPayments', stats.pendingPayments || 0, 'currency');
    animateValue('completedOrders', stats.completedOrders || 0, 'number');
    updateHeroHighlights(stats);
    renderSparklineCharts(stats);
}

/**
 * Update recent orders table
 */
function updateRecentOrders(orders) {
    const tbody = $('#recentOrdersTable');
    tbody.empty();

    if (orders.length === 0) {
        tbody.html('<tr><td colspan="4" class="text-center">No orders found</td></tr>');
        return;
    }

    orders.forEach(order => {
        const statusBadge = getStatusBadge(order.status);
        const row = `
            <tr>
                <td>${order.orderCode}</td>
                <td>${order.customerName || 'N/A'}</td>
                <td>${statusBadge}</td>
                <td>${new Date(order.orderDate).toLocaleDateString()}</td>
            </tr>
        `;
        tbody.append(row);
    });
}

/**
 * Get status badge HTML
 */
function getStatusBadge(status) {
    const badgeClass = `badge badge-${status.toLowerCase()}`;
    return `<span class="${badgeClass}">${status}</span>`;
}

/**
 * Initialize charts
 */
function buildSparklineSeries(value, multipliers) {
    const base = Math.max(Number(value || 0), 1);
    return multipliers.map(m => Math.max(0, +(base * m).toFixed(2)));
}

function renderSparklineCharts(stats) {
    const chartConfigs = {
        customersSparkline: {
            value: stats.totalCustomers || 0,
            color: '#6366f1',
            fill: 'rgba(99, 102, 241, 0.12)',
            series: buildSparklineSeries(stats.totalCustomers || 0, [0.72, 0.78, 0.8, 0.88, 0.93, 0.97, 1])
        },
        ordersSparkline: {
            value: stats.activeOrders || 0,
            color: '#0ea5e9',
            fill: 'rgba(14, 165, 233, 0.12)',
            series: buildSparklineSeries(stats.activeOrders || 0, [0.5, 0.7, 0.66, 0.82, 0.78, 0.92, 1])
        },
        paymentsSparkline: {
            value: stats.pendingPayments || 0,
            color: '#f59e0b',
            fill: 'rgba(245, 158, 11, 0.14)',
            series: buildSparklineSeries(stats.pendingPayments || 0, [0.9, 0.82, 0.86, 0.8, 0.76, 0.84, 1])
        },
        completedSparkline: {
            value: stats.completedOrders || 0,
            color: '#22c55e',
            fill: 'rgba(34, 197, 94, 0.12)',
            series: buildSparklineSeries(stats.completedOrders || 0, [0.45, 0.58, 0.63, 0.72, 0.84, 0.9, 1])
        }
    };

    Object.entries(chartConfigs).forEach(([canvasId, cfg]) => {
        const canvas = document.getElementById(canvasId);
        if (!canvas || typeof Chart === 'undefined') return;

        if (sparklineInstances[canvasId]) {
            sparklineInstances[canvasId].destroy();
        }

        sparklineInstances[canvasId] = new Chart(canvas, {
            type: 'line',
            data: {
                labels: cfg.series.map((_, idx) => idx + 1),
                datasets: [{
                    data: cfg.series,
                    borderColor: cfg.color,
                    backgroundColor: cfg.fill,
                    fill: true,
                    borderWidth: 2,
                    tension: 0.4,
                    pointRadius: 0,
                    pointHoverRadius: 0
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { display: false }, tooltip: { enabled: false } },
                scales: { x: { display: false }, y: { display: false } },
                elements: { line: { capBezierPoints: true } }
            }
        });
    });
}

function initializeCharts() {
    const ctx = document.getElementById('statusChart');
    if (ctx && typeof Chart !== 'undefined') {
        const stats = latestDashboardStats || {};
        const active = Number(stats.activeOrders || 19);
        const completed = Number(stats.completedOrders || 8);
        const pending = Math.max(1, Math.round(active * 0.45));
        const cancelled = Math.max(1, Math.round(completed * 0.18));

        if (statusChartInstance) {
            statusChartInstance.destroy();
        }

        statusChartInstance = new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels: ['Pending', 'In Progress', 'Completed', 'Cancelled'],
                datasets: [{
                    data: [pending, active, completed, cancelled],
                    backgroundColor: ['#f59e0b', '#0ea5e9', '#22c55e', '#ef4444'],
                    borderWidth: 0,
                    hoverOffset: 10
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: true,
                cutout: '68%',
                plugins: {
                    legend: {
                        position: 'bottom',
                        labels: {
                            usePointStyle: true,
                            boxWidth: 10,
                            padding: 16
                        }
                    }
                }
            }
        });
    }
}

/**
 * Setup event listeners
 */
function setupEventListeners() {
    // Event listeners will be added as needed
    // No longer using $.ajaxSetup - error handling is done in makeRequest()
}

// ═══════════════════════════════════════════════════════════════════
//  USER PROFILE — view & edit
// ═══════════════════════════════════════════════════════════════════

/** Cached profile data so we can reset the form without a new request */
let _cachedProfile = null;

/**
 * Generate a 1–2 letter initials string from firstName + lastName (or username)
 */
function getInitials(data) {
    const fn = (data.firstName || '').trim();
    const ln = (data.lastName  || '').trim();
    if (fn && ln) return (fn[0] + ln[0]).toUpperCase();
    if (fn)       return fn.substring(0, 2).toUpperCase();
    if (data.username) return data.username.substring(0, 2).toUpperCase();
    return '?';
}

/**
 * Update header avatar + name from profile data
 */
function updateHeaderAvatar(data) {
    const initials = getInitials(data);
    const fullName = [data.firstName, data.lastName].filter(Boolean).join(' ') || data.username || 'User';

    document.getElementById('userName').textContent = fullName;

    const avatarImg = document.getElementById('userAvatar');
    const avatarInitials = document.getElementById('userAvatarInitials');

    if (data.avatarUrl) {
        if (avatarImg) {
            avatarImg.src = data.avatarUrl;
            avatarImg.style.display = '';
        }
        if (avatarInitials) avatarInitials.style.display = 'none';
    } else {
        if (avatarImg) avatarImg.style.display = 'none';
        if (avatarInitials) {
            avatarInitials.textContent = initials;
            avatarInitials.style.display = 'flex';
        }
    }
}

/**
 * Open the profile offcanvas and load current user data
 */
async function openProfileOffcanvas() {
    const offcanvasEl = document.getElementById('profileOffcanvas');
    if (!offcanvasEl || !window.bootstrap) return;

    const oc = bootstrap.Offcanvas.getOrCreateInstance(offcanvasEl);
    oc.show();

    try {
        const res = await dashboardService.makeRequest('/api/users/me');
        if (res && res.data) {
            _cachedProfile = res.data;
            populateProfileOffcanvas(res.data);
        }
    } catch (err) {
        console.error('Failed to load profile:', err);
        showAppToast('Không thể tải thông tin hồ sơ', 'danger');
    }
}

/**
 * Populate all fields in the profile offcanvas from data object
 */
function populateProfileOffcanvas(data) {
    const initials = getInitials(data);
    const fullName = [data.firstName, data.lastName].filter(Boolean).join(' ') || data.username || '—';

    // Avatar section
    const profImg = document.getElementById('profileAvatarImg');
    const profInitials = document.getElementById('profileAvatarInitials');
    if (data.avatarUrl) {
        profImg.src = data.avatarUrl;
        profImg.style.display = '';
        profInitials.style.cssText = 'display:none!important';
    } else {
        profImg.style.display = 'none';
        profInitials.textContent = initials;
        profInitials.style.cssText = 'display:flex!important;background:linear-gradient(135deg,#1976d2,#7b1fa2);';
    }

    document.getElementById('profileFullName').textContent = fullName;
    document.getElementById('profileUsername').textContent = '@' + (data.username || '');

    // Role badges
    const rolesEl = document.getElementById('profileRolesBadges');
    if (rolesEl && Array.isArray(data.roles) && data.roles.length) {
        rolesEl.innerHTML = data.roles
            .map(r => `<span class="badge rounded-pill bg-primary bg-opacity-10 text-primary me-1">${escHtml(r.name)}</span>`)
            .join('');
    } else if (rolesEl) {
        rolesEl.innerHTML = '';
    }

    // Edit form
    document.getElementById('pfFirstName').value  = data.firstName  || '';
    document.getElementById('pfLastName').value   = data.lastName   || '';
    document.getElementById('pfEmail').value      = data.email      || '';
    document.getElementById('pfPhone').value      = data.phone      || '';
    document.getElementById('pfAvatarUrl').value  = data.avatarUrl  || '';

    // Read-only fields
    document.getElementById('pfUsernameRo').textContent = data.username || '—';
    document.getElementById('pfProvider').textContent   = data.oauthProvider || '—';
    document.getElementById('pfLastLogin').textContent  = data.lastLogin
        ? new Date(data.lastLogin).toLocaleString('vi-VN') : '—';
    document.getElementById('pfCreatedAt').textContent  = data.createdAt
        ? new Date(data.createdAt).toLocaleString('vi-VN') : '—';

    // Hide error banner
    const errEl = document.getElementById('profileFormError');
    if (errEl) { errEl.textContent = ''; errEl.classList.add('d-none'); }

    // Password tab — hide current-password field for OAuth users
    const isOAuth = data.oauthProvider && data.oauthProvider !== 'LOCAL';
    const pwCurrentGroup = document.getElementById('pwCurrentGroup');
    const pwNotice       = document.getElementById('pwOAuthNotice');
    const pwProviderSpan = document.getElementById('pwOAuthProvider');

    if (pwCurrentGroup) pwCurrentGroup.style.display = isOAuth ? 'none' : '';
    if (pwNotice)       pwNotice.classList.toggle('d-none', !isOAuth);
    if (pwProviderSpan) pwProviderSpan.textContent = data.oauthProvider || '';

    // Clear password form
    ['pwCurrent', 'pwNew', 'pwConfirm'].forEach(id => {
        const el = document.getElementById(id);
        if (el) el.value = '';
    });
    const pwErr  = document.getElementById('pwFormError');
    const pwSucc = document.getElementById('pwFormSuccess');
    if (pwErr)  { pwErr.textContent  = ''; pwErr.classList.add('d-none'); }
    if (pwSucc) { pwSucc.textContent = ''; pwSucc.classList.add('d-none'); }
}

/**
 * Reset edit form back to cached values (undo unsaved edits)
 */
function resetProfileForm() {
    if (_cachedProfile) {
        populateProfileOffcanvas(_cachedProfile);
        showAppToast('Đã hoàn tác thay đổi', 'info');
    }
}

/**
 * Save profile changes
 */
async function saveProfileChanges() {
    const errEl = document.getElementById('profileFormError');
    errEl.classList.add('d-none');

    const payload = {
        firstName: document.getElementById('pfFirstName').value.trim(),
        lastName:  document.getElementById('pfLastName').value.trim(),
        email:     document.getElementById('pfEmail').value.trim(),
        phone:     document.getElementById('pfPhone').value.trim(),
        avatarUrl: document.getElementById('pfAvatarUrl').value.trim()
    };

    // Basic client-side validation
    if (!payload.email) {
        showProfileError('Email không được để trống.'); return;
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(payload.email)) {
        showProfileError('Định dạng email không hợp lệ.'); return;
    }

    const saveBtn = document.querySelector('#pane-info .btn-primary');
    if (saveBtn) { saveBtn.disabled = true; saveBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>Đang lưu...'; }

    try {
        const res = await dashboardService.makeRequest('/api/users/me', {
            method: 'PUT',
            body: JSON.stringify(payload)
        });

        if (res && res.data) {
            _cachedProfile = { ..._cachedProfile, ...res.data };
            populateProfileOffcanvas(_cachedProfile);
            updateHeaderAvatar(_cachedProfile);
            showAppToast('Cập nhật hồ sơ thành công!', 'success');
        }
    } catch (err) {
        const msg = await extractErrorMessage(err, 'Không thể cập nhật hồ sơ');
        showProfileError(msg);
    } finally {
        if (saveBtn) { saveBtn.disabled = false; saveBtn.innerHTML = '<i class="fas fa-save me-1"></i>Lưu thay đổi'; }
    }
}

/**
 * Change password
 */
async function changeUserPassword() {
    const pwErr  = document.getElementById('pwFormError');
    const pwSucc = document.getElementById('pwFormSuccess');
    pwErr.classList.add('d-none');
    pwSucc.classList.add('d-none');

    const isOAuth = _cachedProfile && _cachedProfile.oauthProvider !== 'LOCAL';
    const currentPw = document.getElementById('pwCurrent').value;
    const newPw     = document.getElementById('pwNew').value;
    const confirmPw = document.getElementById('pwConfirm').value;

    if (!isOAuth && !currentPw) {
        showPasswordError('Vui lòng nhập mật khẩu hiện tại.'); return;
    }
    if (!newPw || newPw.length < 6) {
        showPasswordError('Mật khẩu mới phải có ít nhất 6 ký tự.'); return;
    }
    if (newPw !== confirmPw) {
        showPasswordError('Xác nhận mật khẩu không khớp.'); return;
    }

    const btn = document.querySelector('#pane-password .btn-warning');
    if (btn) { btn.disabled = true; btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>Đang xử lý...'; }

    try {
        await dashboardService.makeRequest('/api/users/me/change-password', {
            method: 'POST',
            body: JSON.stringify({
                currentPassword:    currentPw,
                newPassword:        newPw,
                confirmNewPassword: confirmPw
            })
        });

        ['pwCurrent', 'pwNew', 'pwConfirm'].forEach(id => {
            const el = document.getElementById(id);
            if (el) el.value = '';
        });
        pwSucc.textContent = 'Đổi mật khẩu thành công!';
        pwSucc.classList.remove('d-none');
        showAppToast('Đổi mật khẩu thành công!', 'success');
    } catch (err) {
        const msg = await extractErrorMessage(err, 'Không thể đổi mật khẩu');
        showPasswordError(msg);
    } finally {
        if (btn) { btn.disabled = false; btn.innerHTML = '<i class="fas fa-key me-1"></i>Đổi mật khẩu'; }
    }
}

/** Toggle password field visibility */
function togglePwVisibility(inputId, btn) {
    const input = document.getElementById(inputId);
    if (!input) return;
    if (input.type === 'password') {
        input.type = 'text';
        btn.innerHTML = '<i class="fas fa-eye-slash"></i>';
    } else {
        input.type = 'password';
        btn.innerHTML = '<i class="fas fa-eye"></i>';
    }
}

/** Display profile form error */
function showProfileError(msg) {
    const el = document.getElementById('profileFormError');
    if (el) { el.textContent = msg; el.classList.remove('d-none'); }
}

/** Display password form error */
function showPasswordError(msg) {
    const el = document.getElementById('pwFormError');
    if (el) { el.textContent = msg; el.classList.remove('d-none'); }
}

/** Extract a human-readable message from a fetch error / API response */
async function extractErrorMessage(err, fallback) {
    try {
        if (err.response) {
            const body = await err.response.json();
            return body.message || fallback;
        }
    } catch (_) { /* ignore */ }
    return err.message || fallback;
}

/** Escape HTML to prevent XSS when injecting dynamic text into innerHTML */
function escHtml(str) {
    return String(str ?? '').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}

