FROM python:3.11-slim

WORKDIR /app

# Copy requirements first for better caching
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy backend
COPY backend/ ./backend/

# Copy frontend
COPY frontend/ ./frontend/

# Copy entry point
COPY backend/main.py ./main.py

EXPOSE 7654

CMD ["python3", "backend/main.py"]
