-- Demo password for all four accounts: ChatFlow123!
-- Hash format: pbkdf2$iterations$URL-safe-base64-salt$URL-safe-base64-hash
INSERT INTO chatflow.utilisateur
    (numero_telephone, mot_de_passe, pseudo, nom_affiche, statut)
VALUES
    ('+212600000001', 'pbkdf2$120000$Y2hhdGZsb3ctZGVtby1zYXJhLTIwMjY$fkK2ahy6yiEdCSW8aKkcC3tZ9Fs5VKWdsS353OSX9n8', 'sara_demo', 'Sara Demo', 'HORS_LIGNE'),
    ('+212600000002', 'pbkdf2$120000$Y2hhdGZsb3ctZGVtby15YXNzaW5lLTIwMjY$zIE9TFQsMz-brOzrrH2ONXq1e3il2La8vTR8L0oo2l8', 'yassine_demo', 'Yassine Demo', 'HORS_LIGNE'),
    ('+212600000003', 'pbkdf2$120000$Y2hhdGZsb3ctZGVtby1hbWluYS0yMDI2$z8DTxM1Cyidt2T2a1T8PAgJFNHHEqujLAQTZGO3YE28', 'amina_demo', 'Amina Demo', 'HORS_LIGNE'),
    ('+212600000004', 'pbkdf2$120000$Y2hhdGZsb3ctZGVtby1vbWFyLTIwMjY$4qZu0nbwrvG_DvlF_5zmmSmhKMLzP1GB_x5eWlui2Ds', 'omar_demo', 'Omar Demo', 'HORS_LIGNE')
ON CONFLICT DO NOTHING;
