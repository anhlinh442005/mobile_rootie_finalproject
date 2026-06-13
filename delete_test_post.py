import firebase_admin
from firebase_admin import credentials
from firebase_admin import firestore

# Initialize Firebase Admin SDK
cred = credentials.Certificate('serviceAccountKey.json')
firebase_admin.initialize_app(cred)

db = firestore.client()

# Query for the post
posts_ref = db.collection('community_posts')
docs = posts_ref.stream()

deleted_count = 0
for doc in docs:
    data = doc.to_dict()
    content = data.get('content', '')
    if 'tesr tín năng' in content or 'bài này tesr' in content:
        print(f"Deleting doc {doc.id} with content: {content}")
        db.collection('community_posts').document(doc.id).delete()
        deleted_count += 1

print(f"Deleted {deleted_count} test posts.")
