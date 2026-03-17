# Food Logger - Setup Guide

## Prerequisites
- Docker & Docker Compose (for containerized deployment)
- Python 3.11+ (for local development)

## Local Development Setup

### 1. Install Python dependencies
```bash
cd /path/to/foodLoggerNew
pip install -r requirements.txt
```

### 2. Seed test data
```bash
cd backend
python seed.py
```

### 3. Run backend server
```bash
cd backend
python -m uvicorn main:app --reload --host 0.0.0.0 --port 8000
```

The app will be available at `http://localhost:8000`

## Docker Deployment

### Build and run with Docker Compose
```bash
docker-compose up --build
```

Access at `http://localhost:8000`

### Or build standalone image
```bash
docker build -t foodlogger .
docker run -p 8000:8000 -v foodlogger_data:/app/data foodlogger
```

## NAS Deployment

1. Copy the entire project to NAS or where Docker is available
2. Run: `docker-compose up -d`
3. Access via NAS IP: `http://<nas-ip>:8000`

### Data Persistence
- Database file stored in `/app/data/foodlogger.db`
- Mount to NAS volume for persistence: `-v /mnt/nas/foodlogger:/app/data`

## API Endpoints

### Products
- `GET /api/products` - List all products
- `GET /api/products/{barcode}` - Get product by barcode
- `POST /api/products` - Create product
- `PATCH /api/products/{barcode}` - Update product
- `DELETE /api/products/{barcode}` - Delete product

### Inventory
- `GET /api/inventory` - List inventory
- `GET /api/inventory/{id}` - Get item
- `POST /api/inventory` - Add item
- `PATCH /api/inventory/{id}` - Update item
- `DELETE /api/inventory/{id}` - Delete item

### Recipes
- `GET /api/recipes` - List recipes
- `GET /api/recipes/{id}` - Get recipe with ingredients
- `POST /api/recipes` - Create recipe
- `PATCH /api/recipes/{id}` - Update recipe
- `DELETE /api/recipes/{id}` - Delete recipe

## Features (MVP)

✅ **Inventory Management**
- Add inventory items with product reference
- Filter by storage location (fridge, freezer, pantry)
- Sort by expiry date, storage location, or date added
- Track quantity, unit, bought date, expiry date, storage location
- Optional name override for display

✅ **Recipe Management**
- Create recipes with multiple ingredients
- Ingredients reference products (not specific inventory items)
- Time type categorization (very_fast, moderate, slow)
- View recipe ingredient composition

✅ **Product Management** (Admin)
- Create products with barcode, name, brand, category
- Store nutritional information (kcal, protein, carbs, fat)
- Search/filter products
- Delete products

✅ **Database**
- SQLite backend with SQLAlchemy ORM
- Relationships between products, inventory, recipes
- Auto-timestamps on creation

## Phase 2 Features (Future)
- 📱 Barcode scanning with Quagga.js
- 🌍 Open Food Facts API integration for product lookup
- 📊 Recipe feasibility analysis (check if ingredients in inventory)
- ⏰ Expiry date notifications
- 📈 Purchase history tracking
- 👤 Multi-user support

## Troubleshooting

**Port 8000 already in use:**
```bash
docker-compose down  # Stop existing container
docker-compose up --build
```

**Database errors:**
- Delete `backend/data/foodlogger.db` and restart
- Or run `python backend/seed.py` to reinitialize

**Frontend not loading:**
- Ensure backend is running: check `http://localhost:8000/health`
- Check browser console for API errors
- Verify CORS is enabled in backend

## Development Notes

- Frontend: Vanilla HTML/CSS/JS + Tailwind CSS (no build step needed)
- Backend: FastAPI with SQLAlchemy ORM
- Database: SQLite (single-user, NAS-friendly)
- Form validation: Client-side + server-side
- API responses: JSON with consistent error handling
