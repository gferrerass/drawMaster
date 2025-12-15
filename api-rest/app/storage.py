import os
from google.cloud import storage

class GCSClient:
    def __init__(self, bucket_name: str):
        self.bucket_name = bucket_name
        self.client = storage.Client()
        self.bucket = self.client.bucket(bucket_name)

    def upload_file(self, file_stream, destination_blob_name, content_type=None):
        blob = self.bucket.blob(destination_blob_name)
        blob.upload_from_file(file_stream, content_type=content_type)
        # Make public URL (requires bucket permissions) or return blob public url
        try:
            blob.make_public()
        except Exception:
            pass
        return blob.public_url
