'use strict';

const byId = id => document.getElementById(id);
const search = byId('search');
const message = byId('message');
const send = byId('send');
const drafts = new Map();
const pending = new Set();
let accountId;
let selectedId;
let conversations = [];

// Conserver un seul contexte audio
let audioContext = null;

function playSendSound() {
  try {
    const AudioContextClass =
        window.AudioContext || window.webkitAudioContext;

    if (!AudioContextClass) {
      console.warn("AudioContext n'est pas disponible.");
      return;
    }

    // Créer le contexte une seule fois
    if (!audioContext) {
      audioContext = new AudioContextClass();
    }

    const play = () => {
      const oscillator = audioContext.createOscillator();
      const gain = audioContext.createGain();

      oscillator.type = "sine";

      oscillator.frequency.setValueAtTime(
          750,
          audioContext.currentTime
      );

      oscillator.frequency.exponentialRampToValueAtTime(
          450,
          audioContext.currentTime + 0.10
      );

      gain.gain.setValueAtTime(
          0.0001,
          audioContext.currentTime
      );

      gain.gain.exponentialRampToValueAtTime(
          0.15,
          audioContext.currentTime + 0.01
      );

      gain.gain.exponentialRampToValueAtTime(
          0.0001,
          audioContext.currentTime + 0.15
      );

      oscillator.connect(gain);
      gain.connect(audioContext.destination);

      oscillator.onended = () => {
        oscillator.disconnect();
        gain.disconnect();
      };

      oscillator.start();
      oscillator.stop(audioContext.currentTime + 0.16);
    };

    if (audioContext.state === "suspended") {
      audioContext.resume().then(play).catch(error => {
        console.error("Erreur audio :", error);
      });
    } else {
      play();
    }

  } catch (error) {
    console.error("Impossible de jouer le son :", error);
  }
}


function element(tag, classes, text) {
  const node = document.createElement(tag);
  if (classes) node.className = classes;
  if (text != null) node.textContent = text;
  return node;
}

function initials(name) {
  return name.trim().split(/\s+/).slice(0, 2)
    .map(word => Array.from(word)[0] || '').join('').toLocaleUpperCase('fr') || '?';
}

function conversationName(conversation) {
  return conversation.displayName || 'Conversation n° ' + conversation.id;
}

function showIdentity(id, name, username) {
  accountId = id;
  byId('account-avatar').textContent = initials(name);
  byId('account-name').textContent = name;
  byId('account-detail').textContent = 'Connecté · @' + username;
}

function showStatus(message) {
  byId('status').textContent = message;
  byId('status').hidden = !message;
}

function dateKey(date) {
  return date.toLocaleDateString('fr-FR');
}

function dayLabel(date) {
  const today = new Date();
  if (dateKey(date) === dateKey(today)) return 'Aujourd’hui';
  today.setDate(today.getDate() - 1);
  if (dateKey(date) === dateKey(today)) return 'Hier';
  return date.toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' });
}

function timeLabel(date) {
  return date.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
}

function renderConversations() {
  const list = byId('conversations');
  const focusedId = document.activeElement.dataset.conversation;
  list.textContent = '';
  if (!conversations.length) {
    list.append(element('li', 'text-sm text-muted', 'Aucune conversation pour le moment.'));
    return;
  }

  for (const conversation of conversations) {
    const active = conversation.id === selectedId;
    const name = conversationName(conversation);
    const date = new Date(conversation.lastActivity);
    const row = element('li');
    const button = element('button', 'conversation-row');
    button.type = 'button';
    button.dataset.conversation = conversation.id;
    if (active) button.setAttribute('aria-current', 'true');
    button.addEventListener('click', () => window.chatflow.select(conversation.id));

    const description = element('span', 'min-w-0 flex-1');
    description.append(
      element('span', 'block truncate text-[13px] font-semibold', name),
      element('span', 'block truncate text-[11px] text-muted', conversation.lastMessage || 'Démarrer la discussion')
    );
    const metadata = element('span', 'flex shrink-0 flex-col items-end gap-0.5 text-[10px] text-muted');
    if (conversation.lastActivity) {
      metadata.append(element('span', '', dateKey(date) === dateKey(new Date()) ? timeLabel(date) : dayLabel(date)));
    }
    if (!active && Number(conversation.unreadCount) > 0) {
      const badge = element('span', 'rounded-full bg-action px-1.5 text-white', conversation.unreadCount);
      badge.setAttribute('aria-label', conversation.unreadCount + ' messages non lus');
      metadata.append(badge);
    }
    button.append(element('span', 'avatar conversation-avatar h-[34px] w-[34px]', initials(name)), description, metadata);
    row.append(button);
    list.append(row);
    if (focusedId === conversation.id) button.focus();
  }
}

function updateConversations(payload) {
  conversations = JSON.parse(payload);
  const current = conversations.find(conversation => conversation.id === selectedId);
  if (current) showContact(conversationName(current), current.username, current.status);
  renderConversations();
}

function showContact(name, username, status) {
  const avatar = byId('contact-avatar');
  const detail = byId('contact-detail');
  avatar.hidden = false;
  avatar.textContent = initials(name);
  byId('contact-name').textContent = name;
  detail.classList.toggle('text-success', status === 'EN_LIGNE');
  detail.textContent = status === 'EN_LIGNE' ? '●  En ligne' :
    status === 'HORS_LIGNE' ? 'Hors ligne' : username ? '@' + username : '';
}

function selectConversation(id, name, username, status) {
  if (selectedId !== id) {
    if (selectedId) drafts.set(selectedId, message.value);
    selectedId = id;
    message.value = drafts.get(id) || '';
    message.disabled = false;
    send.disabled = pending.has(id);
    byId('messages').replaceChildren(element('p', 'text-center text-sm text-muted', 'Chargement…'));
  }
  showContact(name, username, status);
  renderConversations();
}

function showMessages(id, payload) {
  if (id !== selectedId) return;
  const rows = JSON.parse(payload).sort((a, b) => {
    return new Date(a.sentAt).getTime() -
        new Date(b.sentAt).getTime();
  });
  const history = byId('history');
  const list = byId('messages');
  const previousHeight = history.scrollHeight;
  const atBottom = previousHeight - history.scrollTop - history.clientHeight < 40;
  const previousTop = history.scrollTop;
  list.textContent = '';
  if (!rows.length) {
    list.append(element('p', 'text-center text-sm text-muted', 'Aucun message pour le moment.'));
    return;
  }

  let previousDay;
  for (const entry of rows) {
    const mine = entry.senderId === accountId;
    const date = new Date(entry.sentAt);
    const day = dateKey(date);
    if (day !== previousDay) {
      list.append(element('p', 'text-center text-[11px] font-semibold text-muted', dayLabel(date)));
      previousDay = day;
    }

    const line = element('div', mine ? 'message-line justify-end' : 'message-line');
    if (!mine) {
      const avatar = element('span', 'avatar h-[25px] w-[25px] text-xs', initials(entry.displayName || entry.username));
      avatar.title = entry.displayName || entry.username;
      line.append(avatar);
    }
    const bubble = element('div', mine ? 'message-bubble message-sent' : 'message-bubble');
    bubble.append(
      element('p', 'whitespace-pre-wrap', entry.content),
      element('p', mine ? 'text-[10px] text-white' : 'text-[10px] text-muted', timeLabel(date))
    );
    line.append(bubble);
    list.append(line);
  }
  history.scrollTop = atBottom ? history.scrollHeight : previousTop;
}

function action(label, handler, disabled = false) {
  const button = element('button', 'contact-action', label);
  button.type = 'button';
  button.disabled = disabled;
  button.addEventListener('click', handler);
  return button;
}

function renderPeople(listId, people, empty, buttons) {
  const list = byId(listId);
  list.textContent = '';
  if (!people.length) list.append(element('li', 'py-2 text-xs text-muted', empty));
  for (const person of people) {
    const row = element('li', 'border-b border-line py-2');
    const description = element('p', 'break-words text-[13px] font-semibold', person.displayName);
    const username = element('p', 'break-words text-[11px] text-muted', '@' + person.username);
    const actions = element('div', 'mt-1 flex flex-wrap gap-2');
    actions.append(...buttons(person));
    row.append(description, username, actions);
    list.append(row);
  }
}

function messageContact(person) {
  return action('Message', () => window.chatflow.openDirect(person.id, person.displayName, person.username, person.status || ''));
}

function showSearch(payload) {
  byId('conversations').hidden = true;
  byId('conversations').classList.remove('flex');
  byId('people').hidden = false;
  renderPeople('search-results', JSON.parse(payload), 'Aucun compte trouvé.', person => [
    messageContact(person),
    action('Ajouter', () => window.chatflow.changeContact('add', person.id), person.isFriend || Boolean(person.pendingStatus)),
  ]);
}

function showContacts(friends, requests) {
  renderPeople('friends', JSON.parse(friends), 'Aucun contact.', person => [
    messageContact(person),
    action('Retirer', () => window.chatflow.changeContact('remove', person.id)),
  ]);
  renderPeople('requests', JSON.parse(requests), 'Aucune demande en attente.', person => [
    action('Accepter', () => window.chatflow.changeContact('accept', person.requestId)),
    action('Refuser', () => window.chatflow.changeContact('decline', person.requestId)),
  ]);
}

function clearSearch() {
  search.value = '';
  byId('people').hidden = true;
  byId('conversations').hidden = false;
  byId('conversations').classList.add('flex');
}

function messageFinished(id, body, successful) {
  pending.delete(id);
  if (successful && (drafts.get(id) || '').trim() === body) drafts.delete(id);
  if (id !== selectedId) return;
  send.disabled = false;
  if (successful && message.value.trim() === body) message.value = '';
}

window.chatflowReady = function () {
  byId('search-form').addEventListener('submit', event => {
    event.preventDefault();
    window.chatflow.search(search.value.trim(), false);
  });
  byId('clear-search').addEventListener('click', () => {
    window.chatflow.cancelSearch();
    clearSearch();
    search.focus();
  });
  byId('composer').addEventListener('submit', event => {
    event.preventDefault();

    const body = message.value.trim();

    if (!selectedId || !body || pending.has(selectedId)) return;

    pending.add(selectedId);
    send.disabled = true;

    // Jouer le petit son d'envoi
    playSendSound();

    // Envoyer le message au serveur
    window.chatflow.send(body);
  });
  byId('logout').addEventListener('click', () => {
    byId('logout').disabled = true;
    window.chatflow.logout();
  });
  window.chatflow.ready();
};

function historyFailed(id) {
  if (id === selectedId) {
    byId('messages').replaceChildren(element('p', 'text-center text-sm text-muted', 'Impossible de charger les messages.'));
  }
}

function refreshSearch() {
  if (!byId('people').hidden && search.value.trim().length >= 2) {
    window.chatflow.search(search.value.trim(), true);
  }
}
