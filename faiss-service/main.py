from fastapi import FastAPI
from pydantic import BaseModel
import faiss
import numpy as np
import pickle

app = FastAPI()

index = None
metadata = []
dimension = None

class AddRequest(BaseModel):
    url: str
    embedding: list

class SearchRequest(BaseModel):
    embedding: list
    k: int = 10


@app.post("/add")
def add(req: AddRequest):
    global index, dimension

    vector = np.array(req.embedding).astype("float32")

    if index is None:
        dimension = vector.shape[0]
        index = faiss.IndexFlatL2(dimension)
        print("Initialized FAISS with dim:", dimension)

    if vector.shape[0] != dimension:
        return {"error": f"Dimension mismatch. Expected {dimension}, got {vector.shape[0]}"}

    index.add(vector.reshape(1, -1))
    metadata.append(req.url)

    return {"status": "added"}

@app.post("/search")
def search(req: SearchRequest):
    global index, dimension

    if index is None:
        return {"error": "Index not initialized"}

    vector = np.array(req.embedding).astype("float32")

    if vector.shape[0] != dimension:
        return {"error": f"Query dimension mismatch. Expected {dimension}, got {vector.shape[0]}"}

    distances, indices = index.search(vector.reshape(1, -1), req.k)

    results = []
    for i, idx in enumerate(indices[0]):
        if idx < len(metadata):
            results.append({
                "url": metadata[idx],
                "score": float(distances[0][i])
            })

    return {"results": results}


@app.post("/save")
def save():
    faiss.write_index(index, "faiss.index")
    with open("meta.pkl", "wb") as f:
        pickle.dump(metadata, f)
    return {"status": "saved"}


@app.post("/load")
def load():
    global index, metadata
    index = faiss.read_index("faiss.index")
    with open("meta.pkl", "rb") as f:
        metadata = pickle.load(f)
    return {"status": "loaded"}