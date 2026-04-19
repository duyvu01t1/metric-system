/**
 * FormValidator - Shared validation utility for Bootstrap 5 modal forms.
 * Usage:
 *   const ok = FormValidator.validate('myModalId', [
 *     { id: 'fieldId', label: 'Tên trường', required: true },
 *     { id: 'emailField', label: 'Email', required: true, type: 'email' },
 *     { id: 'numField', label: 'Số tiền', required: true, type: 'number', min: 0 },
 *   ]);
 *   if (!ok) return;
 */
window.FormValidator = (function () {

    /**
     * Remove all validation error states inside a container element.
     * @param {string} containerId - the id of the modal or form wrapper
     */
    function clearErrors(containerId) {
        const container = document.getElementById(containerId);
        if (!container) return;
        container.querySelectorAll('.is-invalid').forEach(function (el) {
            el.classList.remove('is-invalid');
        });
        container.querySelectorAll('.fv-invalid-feedback').forEach(function (el) {
            el.remove();
        });
    }

    /**
     * Mark a single field as invalid, appending an inline error message.
     * @param {HTMLElement} el - the input/select/textarea element
     * @param {string} message - error message to display
     */
    function markInvalid(el, message) {
        el.classList.add('is-invalid');
        // Remove any existing feedback for this field
        const existing = el.parentElement.querySelector('.fv-invalid-feedback');
        if (existing) existing.remove();
        const fb = document.createElement('div');
        fb.className = 'invalid-feedback fv-invalid-feedback d-block';
        fb.textContent = message;
        el.parentElement.appendChild(fb);
    }

    /**
     * Validate fields inside a modal/container.
     * @param {string} containerId - element id of the modal or form wrapper
     * @param {Array}  rules       - array of rule objects
     * @returns {boolean} true if all validations pass
     */
    function validate(containerId, rules) {
        clearErrors(containerId);
        var valid = true;
        rules.forEach(function (rule) {
            var el = document.getElementById(rule.id);
            if (!el) return; // skip missing fields
            var raw = el.value;
            var val = (typeof raw === 'string') ? raw.trim() : raw;

            // Required check
            if (rule.required && !val) {
                markInvalid(el, (rule.label || rule.id) + ' là bắt buộc');
                valid = false;
                return;
            }

            if (!val) return; // optional and empty — skip further checks

            // Email format
            if (rule.type === 'email') {
                var emailRe = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
                if (!emailRe.test(val)) {
                    markInvalid(el, (rule.label || rule.id) + ' không đúng định dạng email');
                    valid = false;
                    return;
                }
            }

            // Numeric check (number / money)
            if (rule.type === 'number') {
                var num = parseFloat(val);
                if (isNaN(num)) {
                    markInvalid(el, (rule.label || rule.id) + ' phải là số');
                    valid = false;
                    return;
                }
                if (rule.min !== undefined && num < rule.min) {
                    markInvalid(el, (rule.label || rule.id) + ' phải >= ' + rule.min);
                    valid = false;
                    return;
                }
                if (rule.max !== undefined && num > rule.max) {
                    markInvalid(el, (rule.label || rule.id) + ' phải <= ' + rule.max);
                    valid = false;
                    return;
                }
            }

            // Select — ensure a non-empty option is chosen
            if (el.tagName === 'SELECT' && rule.required && !val) {
                markInvalid(el, 'Vui lòng chọn ' + (rule.label || rule.id));
                valid = false;
                return;
            }

            // Custom comparator (e.g. date range)
            if (rule.compare) {
                var otherEl = document.getElementById(rule.compare.id);
                if (otherEl && otherEl.value) {
                    var a = val, b = otherEl.value.trim();
                    if (rule.compare.op === 'gte' && a < b) {
                        markInvalid(el, (rule.label || rule.id) + ' phải >= ' + (rule.compare.label || rule.compare.id));
                        valid = false;
                    }
                    if (rule.compare.op === 'lte' && a > b) {
                        markInvalid(el, (rule.label || rule.id) + ' phải <= ' + (rule.compare.label || rule.compare.id));
                        valid = false;
                    }
                }
            }
        });
        return valid;
    }

    return { validate: validate, clearErrors: clearErrors };
})();
