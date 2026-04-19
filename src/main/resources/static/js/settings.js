// Settings Service Class
class SettingsService {
    constructor() {
        this.currentCategory = 'GENERAL';
        this.settingsCache = {};
        this.modifiedSettings = new Set();
    }

    async makeRequest(url, options = {}) {
        const token = window.getStoredAuthToken
            ? window.getStoredAuthToken()
            : (localStorage.getItem('auth_token') || localStorage.getItem('authToken') || localStorage.getItem('token'));
        const headers = {
            'Content-Type': 'application/json',
            ...options.headers
        };
        
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }

        try {
            const response = await fetch(url, {
                ...options,
                headers: headers,
                credentials: 'same-origin'
            });

            if (!response.ok) {
                const error = await response.json();
                throw new Error(error.message || 'Request failed');
            }

            return await response.json();
        } catch (error) {
            console.error('Request error:', error);
            throw error;
        }
    }

    async getAllSettings() {
        try {
            const response = await this.makeRequest('/api/settings');
            if (response.data) {
                response.data.forEach(setting => {
                    this.settingsCache[setting.id] = setting;
                });
            }
            return response.data || [];
        } catch (error) {
            console.error('Error fetching settings:', error);
            throw error;
        }
    }

    async getSettingsByCategory(category) {
        try {
            const response = await this.makeRequest(`/api/settings/category/${category}`);
            if (response.data) {
                response.data.forEach(setting => {
                    this.settingsCache[setting.id] = setting;
                });
            }
            return response.data || [];
        } catch (error) {
            console.error('Error fetching settings by category:', error);
            throw error;
        }
    }

    async getSettingByKey(key) {
        try {
            const response = await this.makeRequest(`/api/settings/key/${key}`);
            if (response.data) {
                this.settingsCache[response.data.id] = response.data;
            }
            return response.data;
        } catch (error) {
            console.error('Error fetching setting:', error);
            throw error;
        }
    }

    async updateSetting(id, settingData) {
        try {
            const response = await this.makeRequest(`/api/settings/${id}`, {
                method: 'PUT',
                body: JSON.stringify(settingData)
            });
            if (response.data) {
                this.settingsCache[id] = response.data;
                this.modifiedSettings.delete(id);
            }
            return response.data;
        } catch (error) {
            console.error('Error updating setting:', error);
            throw error;
        }
    }

    async updateSettingByKey(key, value) {
        try {
            const response = await this.makeRequest(`/api/settings/key/${key}`, {
                method: 'PUT',
                body: JSON.stringify({ settingValue: value })
            });
            if (response.data) {
                this.settingsCache[response.data.id] = response.data;
                this.modifiedSettings.delete(response.data.id);
            }
            return response.data;
        } catch (error) {
            console.error('Error updating setting by key:', error);
            throw error;
        }
    }

    async createSetting(settingData) {
        try {
            const response = await this.makeRequest('/api/settings', {
                method: 'POST',
                body: JSON.stringify(settingData)
            });
            if (response.data) {
                this.settingsCache[response.data.id] = response.data;
            }
            return response.data;
        } catch (error) {
            console.error('Error creating setting:', error);
            throw error;
        }
    }

    markModified(id) {
        this.modifiedSettings.add(id);
    }

    getModifiedCount() {
        return this.modifiedSettings.size;
    }
}

// Initialize service
const settingsService = new SettingsService();

// Category configuration
const categoryConfig = {
    GENERAL: {
        title: 'General Settings',
        description: 'General application configuration'
    },
    BUSINESS: {
        title: 'Business Settings',
        description: 'Shop business information and preferences'
    },
    EMAIL: {
        title: 'Email Settings',
        description: 'Email service configuration'
    },
    PAYMENT: {
        title: 'Payment Settings',
        description: 'Payment method configuration'
    },
    SYSTEM: {
        title: 'System Settings',
        description: 'System-level configuration'
    }
};

// Data type mapping
const dataTypeMap = {
    'STRING': 'text',
    'INT': 'number',
    'DECIMAL': 'number',
    'BOOLEAN': 'checkbox',
    'JSON': 'textarea'
};

/**
 * Initialize function for settings page
 */
function initSettingsPage() {
    loadSettings('GENERAL');
}

/**
 * Initialize page
 */
document.addEventListener('DOMContentLoaded', function() {
    initSettingsPage();
});

/**
 * Support for dynamic page loading
 */
document.addEventListener('pageLoaded', function(event) {
    if (event.detail && event.detail.page === 'settings') {
        initSettingsPage();
    }
});


// Switch category
async function switchCategory(category) {
    // Remove active class from all categories
    document.querySelectorAll('.settings-category').forEach(el => {
        el.classList.remove('active');
    });

    // Add active class to clicked category
    event.target.closest('.settings-category').classList.add('active');

    // Load settings for the category
    await loadSettings(category);
}

// Load settings for a category
async function loadSettings(category) {
    try {
        settingsService.currentCategory = category;
        const config = categoryConfig[category];
        
        // Update title and description
        document.getElementById('categoryTitle').textContent = config.title;
        document.getElementById('categoryDescription').textContent = config.description;

        // Load settings
        const settings = await settingsService.getSettingsByCategory(category);
        displaySettings(settings);

        // Hide any previous alerts
        hideAlert();
    } catch (error) {
        showError('Failed to load settings: ' + error.message);
    }
}

// Display settings
function displaySettings(settings) {
    const container = document.getElementById('settingsContainer');
    
    if (!settings || settings.length === 0) {
        container.innerHTML = `
            <div class="alert alert-info">
                <i class="fas fa-info-circle"></i> No settings available in this category
            </div>
        `;
        return;
    }

    let html = '';
    
    settings.forEach(setting => {
        const inputType = dataTypeMap[setting.dataType] || 'text';
        const isEditable = setting.isEditable && setting.dataType !== 'JSON';
        const classModifier = isEditable ? 'editable' : '';
        
        let inputField = '';
        if (isEditable) {
            if (inputType === 'checkbox') {
                const checked = setting.settingValue === 'true' || setting.settingValue === true ? 'checked' : '';
                inputField = `
                    <input type="checkbox" class="form-check-input setting-value" 
                           id="setting_${setting.id}" data-id="${setting.id}"
                           data-key="${setting.settingKey}" ${checked}
                           onchange="markSettingModified(${setting.id})">
                    <label class="form-check-label" for="setting_${setting.id}">Enable this setting</label>
                `;
            } else if (inputType === 'textarea') {
                inputField = `
                    <textarea class="form-control setting-value setting-input" 
                              id="setting_${setting.id}" data-id="${setting.id}"
                              data-key="${setting.settingKey}"
                              onchange="markSettingModified(${setting.id})"
                              rows="4">${escapeHtml(setting.settingValue)}</textarea>
                `;
            } else {
                inputField = `
                    <input type="${inputType}" class="form-control setting-value setting-input" 
                           id="setting_${setting.id}" data-id="${setting.id}"
                           data-key="${setting.settingKey}"
                           value="${escapeHtml(setting.settingValue)}"
                           onchange="markSettingModified(${setting.id})">
                `;
            }
        } else {
            inputField = `<div class="form-control" style="background-color: #f5f5f5;">${escapeHtml(setting.settingValue)}</div>`;
        }

        html += `
            <div class="setting-item ${classModifier}" data-id="${setting.id}">
                <div class="setting-label">
                    <i class="fas fa-tag"></i> ${escapeHtml(setting.settingKey)}
                    ${isEditable ? '<span class="badge bg-primary ms-2">Editable</span>' : '<span class="badge bg-secondary ms-2">Read-only</span>'}
                </div>
                ${setting.description ? `<p class="setting-description">${escapeHtml(setting.description)}</p>` : ''}
                ${inputField}
                <small class="text-muted">Type: ${setting.dataType}</small>
            </div>
        `;
    });

    container.innerHTML = html;
}

// Mark setting as modified
function markSettingModified(id) {
    settingsService.markModified(id);
    const item = document.querySelector(`[data-id="${id}"]`);
    if (item) {
        item.style.opacity = '0.7';
        item.style.borderLeft = '4px solid #ffc107';
    }
}

// Save all modified settings
async function saveAllSettings() {
    try {
        const modifiedCount = settingsService.getModifiedCount();
        
        if (modifiedCount === 0) {
            showInfo('No changes to save');
            return;
        }

        // Collect modified settings
        const updates = [];
        const inputs = document.querySelectorAll('.setting-value');
        
        inputs.forEach(input => {
            if (settingsService.modifiedSettings.has(parseInt(input.dataset.id))) {
                let value = input.type === 'checkbox' ? input.checked : input.value;
                updates.push({
                    id: input.dataset.id,
                    key: input.dataset.key,
                    value: value
                });
            }
        });

        // Performance: save all together or batch
        let successCount = 0;
        const errors = [];

        for (const update of updates) {
            try {
                await settingsService.updateSetting(update.id, {
                    settingValue: update.value.toString()
                });
                successCount++;
            } catch (error) {
                errors.push(`${update.key}: ${error.message}`);
            }
        }

        // Reset modified tracking
        settingsService.modifiedSettings.clear();

        // Reload settings
        await loadSettings(settingsService.currentCategory);

        if (errors.length === 0) {
            showSuccess(`Successfully saved ${successCount} setting(s)`);
        } else {
            showWarning(`Saved ${successCount} setting(s), but ${errors.length} failed:<br>${errors.join('<br>')}`);
        }
    } catch (error) {
        showError('Failed to save settings: ' + error.message);
    }
}

// Reset to category default
function resetSettings() {
    if (confirm('Reset all changes in this category?')) {
        settingsService.modifiedSettings.clear();
        loadSettings(settingsService.currentCategory);
        showInfo('Settings reset');
    }
}

// Reset all to defaults (admin only)
async function resetAllDefaults() {
    if (!confirm('Are you sure? This will reset ALL settings to factory defaults. This cannot be undone!')) {
        return;
    }

    if (!confirm('This is irreversible. Type your password to confirm.')) {
        return;
    }

    try {
        // This would require a special endpoint on the backend
        showWarning('Reset all defaults feature requires backend implementation');
    } catch (error) {
        showError('Failed to reset defaults: ' + error.message);
    }
}

// Utility functions
function escapeHtml(text) {
    const map = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#039;'
    };
    return String(text || '').replace(/[&<>"']/g, m => map[m]);
}

function showAlert(message, type = 'danger') {
    const alert = document.getElementById(type === 'danger' ? 'errorAlert' : 
                                        type === 'success' ? 'successAlert' : 'errorAlert');
    const messageEl = document.getElementById(type === 'danger' ? 'errorMessage' : 
                                            type === 'success' ? 'successMessage' : 'errorMessage');
    
    messageEl.innerHTML = message;
    alert.classList.remove('hide');
    
    setTimeout(() => {
        alert.classList.add('hide');
    }, 5000);
}

function showError(message) {
    showAlert(message, 'danger');
}

function showSuccess(message) {
    showAlert(message, 'success');
}

function showInfo(message) {
    const container = document.getElementById('settingsContainer');
    const alertHtml = `
        <div class="alert alert-info alert-dismissible fade show" role="alert">
            <i class="fas fa-info-circle"></i> ${escapeHtml(message)}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    `;
    container.innerHTML = alertHtml + container.innerHTML;
}

function showWarning(message) {
    const container = document.getElementById('settingsContainer');
    const alertHtml = `
        <div class="alert alert-warning alert-dismissible fade show" role="alert">
            <i class="fas fa-exclamation-triangle"></i> ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    `;
    container.innerHTML = alertHtml + container.innerHTML;
}

function hideAlert() {
    document.getElementById('errorAlert').classList.add('hide');
    document.getElementById('successAlert').classList.add('hide');
}
