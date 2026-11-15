'use strict';

const phone = document.getElementById('phone');
const password = document.getElementById('password');
const welcome = document.getElementById('welcome');
const openMessenger = document.getElementById('open-messenger');

function authFailed(message) {
  setBusy(false);
  showError(message);
  password.focus();
}

function authSucceeded(name) {
  password.value = '';
  form.hidden = true;
  form.classList.remove('flex');
  const greeting = document.getElementById('greeting');
  greeting.textContent = 'Bienvenue, ' + name;
  welcome.hidden = false;
  greeting.focus();
}

function updateConversationSummary(count) {
  document.getElementById('conversation-summary').textContent = count < 0
    ? 'Retrouvez vos conversations dans la messagerie.'
    : count === 0 ? 'Aucune conversation pour le moment.'
      : count + (count === 1 ? ' conversation' : ' conversations');
}

window.chatflowReady = function () {
  phone.focus();
  form.addEventListener('submit', event => {
    event.preventDefault();
    if (submitButton.disabled) return;

    showError('');
    if (!phone.value.trim() || !password.value) {
      showError('Saisissez votre numéro de téléphone et votre mot de passe.');
      (phone.value.trim() ? password : phone).focus();
      return;
    }

    const number = phone.value.trim();
    const secret = password.value;
    setBusy(true, '◌  Connexion en cours…');
    window.chatflow.login(number, secret);
  });
  alternate.addEventListener('click', () => window.chatflow.openRegistration());
  openMessenger.addEventListener('click', () => window.chatflow.openMessenger());
};
