/**
 * Authentication Module
 * Handles login, logout, and OAuth2 flows
 */

class AuthService {
    constructor() {
        this.apiBaseUrl = '/api/auth';
        this.tokenKey = 'auth_token';
        this.legacyTokenKeys = ['authToken', 'token'];
        this.refreshTokenKey = 'refresh_token';
    }

    /**
     * Make authenticated API request using Fetch API
     */
    async makeRequest(url, options = {}) {
        const headers = {
            'Content-Type': 'application/json',
            ...options.headers
        };

        const token = this.getToken();
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }

        const response = await fetch(url, {
            ...options,
            headers,
            credentials: 'same-origin'
        });

        const data = await response.json();

        if (!response.ok) {
            const error = new Error(data.message || `HTTP error! status: ${response.status}`);
            error.status = response.status;
            error.data = data;
            throw error;
        }

        return data;
    }

    /**
     * Login with username and password
     */
    async login(username, password) {
        try {
            const response = await this.makeRequest(`${this.apiBaseUrl}/login`, {
                method: 'POST',
                body: JSON.stringify({ username, password })
            });

            if (response.success) {
                this.setToken(response.data.token);
                this.setRefreshToken(response.data.refreshToken);
                return response;
            } else {
                throw new Error(response.message || 'Login failed');
            }
        } catch (error) {
            let errorMessage = 'Login failed. Please check your credentials.';
            if (error.message) {
                errorMessage = error.message;
            }
            if (error.status === 401) {
                errorMessage = 'Invalid username or password';
            }
            if (error.status === 0) {
                errorMessage = 'Unable to connect to server';
            }
            throw new Error(errorMessage);
        }
    }

    /**
     * Logout and clear tokens
     */
    async logout() {
        try {
            await this.makeRequest(`${this.apiBaseUrl}/logout`, {
                method: 'POST'
            });
        } catch (error) {
            console.error('Logout error:', error);
        } finally {
            this.clearTokens();
            window.location.href = '/api/pages/login';
        }
    }

    /**
     * Get stored token
     */
    getToken() {
        const token = localStorage.getItem(this.tokenKey)
            || this.legacyTokenKeys.map(key => localStorage.getItem(key)).find(Boolean);

        if (token && !localStorage.getItem(this.tokenKey)) {
            localStorage.setItem(this.tokenKey, token);
        }

        return token;
    }

    /**
     * Set token
     */
    setToken(token) {
        if (!token) {
            return;
        }

        [this.tokenKey, ...this.legacyTokenKeys].forEach(key => {
            localStorage.setItem(key, token);
        });
    }

    /**
     * Get stored refresh token
     */
    getRefreshToken() {
        return localStorage.getItem(this.refreshTokenKey);
    }

    /**
     * Set refresh token
     */
    setRefreshToken(token) {
        localStorage.setItem(this.refreshTokenKey, token);
    }

    /**
     * Clear all tokens
     */
    clearTokens() {
        [this.tokenKey, ...this.legacyTokenKeys, this.refreshTokenKey].forEach(key => {
            localStorage.removeItem(key);
        });
    }

    /**
     * Check if user is authenticated
     */
    isAuthenticated() {
        return !!this.getToken();
    }

    /**
     * Handle authentication errors
     */
    handleError(error) {
        console.error('Authentication error:', error);
        if (error.status === 401) {
            this.clearTokens();
            window.location.href = '/api/pages/login';
        }
    }
}

// Create global instance
const authService = new AuthService();
window.getStoredAuthToken = () => authService.getToken();
window.clearStoredAuthTokens = () => authService.clearTokens();

// Ensure jQuery is loaded and setup global defaults if available
if (typeof jQuery !== 'undefined') {
    jQuery.ajaxSetup({
        beforeSend: function (xhr) {
            const token = authService.getToken();
            if (token) {
                xhr.setRequestHeader('Authorization', `Bearer ${token}`);
            }
        }
    });
}

// Login form handler
$(document).ready(function () {
    const loginForm = $('#loginForm');
    const errorAlert = $('#errorAlert');
    const errorMessage = $('#errorMessage');

    if (loginForm.length) {
        loginForm.on('submit', async function (e) {
            e.preventDefault();
            
            // Hide previous error messages
            errorAlert.addClass('hide');

            const username = $('#username').val().trim();
            const password = $('#password').val();
            const submitBtn = $(this).find('button[type="submit"]');
            const originalText = submitBtn.html();

            // Client-side validation
            if (!username) {
                showLoginError('Please enter your username');
                $('#username').focus();
                return;
            }

            if (!password) {
                showLoginError('Please enter your password');
                $('#password').focus();
                return;
            }

            // Disable button during submit
            submitBtn.prop('disabled', true).html('<span class="spinner-border spinner-border-sm me-2"></span>Signing in...');

            try {
                const result = await authService.login(username, password);
                if (result.success) {
                    // Show success message
                    errorAlert.removeClass('hide alert-danger').addClass('alert-success').show();
                    errorMessage.text('Login successful! Redirecting...');
                    
                    // Redirect after short delay
                    setTimeout(() => {
                        window.location.href = '/api/pages/dashboard';
                    }, 500);
                } else {
                    showLoginError(result.message || 'Login failed. Please try again.');
                }
            } catch (error) {
                showLoginError(error.message || 'Login failed. Please check your credentials.');
                console.error('Login error:', error);
            } finally {
                // Re-enable button
                submitBtn.prop('disabled', false).html(originalText);
            }
        });
        
        // Show error message helper function
        window.showLoginError = function(msg) {
            errorMessage.text(msg);
            errorAlert.removeClass('hide alert-success').addClass('alert-danger').show();
            window.scrollTo(0, 0);
        };
    }
});
