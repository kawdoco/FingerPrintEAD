from fastapi import FastAPI

app = FastAPI(title="BCI Research Lab Attendance API")

@app.get("/")
def root():
    return {"status": "BCI Attendance API running"}