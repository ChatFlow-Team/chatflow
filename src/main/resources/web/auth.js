'use strict';

const form = document.getElementById('auth-form');
const submitButton = document.getElementById('submit');
const alternate = document.getElementById('alternate');
const error = document.getElementById('error');
const submitLabel = submitButton.textContent;

function setBusy(busy, label) {
  form.setAttribute('aria-busy', String(busy));
  form.querySelectorAll('input, button').forEach(control => { control.disabled = busy; });
  submitButton.textContent = busy ? label : submitLabel;
}

function showError(message) {
  error.hidden = !message;
  error.textContent = message ? '⚠  ' + message : '';
}
