'use strict';

const registration = {
  phone: document.getElementById('phone'),
  username: document.getElementById('username'),
  name: document.getElementById('display-name'),
  password: document.getElementById('password'),
  confirmation: document.getElementById('confirmation'),
};

function fieldError(field, message) {
  const hint = document.getElementById(field.id + '-error');
  field.setAttribute('aria-invalid', String(Boolean(message)));
  hint.textContent = message;
  hint.hidden = !message;
}

function validateRegistration({ phone, username, name, password, confirmation }) {
  const fields = Object.values(registration);
  const messages = [
    /^\+?\d{8,15}$/.test(phone) ? '' : 'Saisissez un numéro de téléphone valide.',
    /^[a-z0-9_]{3,32}$/i.test(username) ? '' : 'Utilisez 3 à 32 lettres, chiffres ou tirets bas.',
    name.length > 0 && name.length <= 80 ? '' : 'Saisissez un nom affiché de 1 à 80 caractères.',
    password.trim() && password.length >= 8 ? '' : 'Saisissez au moins 8 caractères.',
    !confirmation ? 'Confirmez votre mot de passe.' :
      confirmation !== password ? 'Les mots de passe ne correspondent pas.' : '',
  ];

  fields.forEach((field, index) => fieldError(field, messages[index]));
  const firstInvalid = messages.findIndex(Boolean);
  if (firstInvalid < 0) return true;
  fields[firstInvalid].focus();
  return false;
}

function registrationFailed(message) {
  setBusy(false);
  showError(message);
  submitButton.focus();
}

window.chatflowReady = function () {
  form.addEventListener('submit', event => {
    event.preventDefault();
    if (submitButton.disabled) return;

    showError('');
    const values = {
      phone: registration.phone.value.trim(),
      username: registration.username.value.trim(),
      name: registration.name.value.trim(),
      password: registration.password.value,
      confirmation: registration.confirmation.value,
    };
    if (!validateRegistration(values)) return;

    setBusy(true, '◌  Inscription en cours…');
    window.chatflow.register(values.phone, values.username, values.name, values.password);
  });

  Object.values(registration).forEach(field => {
    field.addEventListener('input', () => fieldError(field, ''));
  });
  alternate.addEventListener('click', () => window.chatflow.openLogin());
};
