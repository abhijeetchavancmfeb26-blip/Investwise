// =====================================================================
//  InvestWise-Lite :: MongoDB initialisation
//
//  Simplified from the original six collections to five: audit_logs and
//  activity_logs were near-identical ("who did what, when") and are now a
//  single activity_logs collection with optional entity fields.
// =====================================================================

const db = db.getSiblingDB('investwise_logs');

db.createUser({
  user: 'investwise',
  pwd: 'investwise123',
  roles: [{ role: 'readWrite', db: 'investwise_logs' }]
});

['activity_logs', 'contact_messages', 'email_logs', 'notifications', 'recommendation_history']
  .forEach(name => {
    if (!db.getCollectionNames().includes(name)) db.createCollection(name);
  });

db.activity_logs.createIndex({ userId: 1, createdAt: -1 });
db.activity_logs.createIndex({ action: 1 });
db.activity_logs.createIndex({ createdAt: 1 }, { expireAfterSeconds: 31536000 }); // 1 year

db.contact_messages.createIndex({ status: 1, createdAt: -1 });
db.contact_messages.createIndex({ email: 1 });

db.email_logs.createIndex({ recipient: 1, createdAt: -1 });
db.email_logs.createIndex({ status: 1 });

db.notifications.createIndex({ userId: 1, read: 1, createdAt: -1 });
db.notifications.createIndex({ createdAt: 1 }, { expireAfterSeconds: 7776000 }); // 90 days

db.recommendation_history.createIndex({ userId: 1, createdAt: -1 });

print('[InvestWise-Lite] MongoDB ready: 5 collections with indexes.');
