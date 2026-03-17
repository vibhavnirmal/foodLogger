from fastapi import FastAPI, Depends, HTTPException, Query
from fastapi.staticfiles import StaticFiles
from fastapi.responses import FileResponse
from sqlalchemy.orm import Session
from datetime import datetime
from pathlib import Path
from pydantic import BaseModel
from typing import Optional, List
import sys

# Add backend to path
sys.path.insert(0, str(Path(__file__).parent))

from database import init_db, get_db, Product, Inventory, Recipe, RecipeIngredient, TimeTypeEnum, engine
from config import DEBUG, PORT, HOST
from services.product_service import fetch_product_from_open_food_facts

# ============================================================================
# PYDANTIC MODELS
# ============================================================================

class ProductCreate(BaseModel):
    barcode: str
    name: str
    brand: Optional[str] = None
    category: Optional[str] = None
    serving_size: Optional[str] = None
    kcal: Optional[float] = None
    protein: Optional[float] = None
    carbs: Optional[float] = None
    fat: Optional[float] = None

class ProductUpdate(BaseModel):
    name: Optional[str] = None
    brand: Optional[str] = None
    category: Optional[str] = None
    serving_size: Optional[str] = None
    kcal: Optional[float] = None
    protein: Optional[float] = None
    carbs: Optional[float] = None
    fat: Optional[float] = None

class InventoryCreate(BaseModel):
    barcode: str
    quantity: float = 1.0
    unit: str = "unit"
    date_bought: Optional[datetime] = None
    expiry_date: Optional[datetime] = None
    storage_location: Optional[str] = None
    name_override: Optional[str] = None
    almost_finished: bool = False

class InventoryUpdate(BaseModel):
    quantity: Optional[float] = None
    unit: Optional[str] = None
    date_bought: Optional[datetime] = None
    expiry_date: Optional[datetime] = None
    storage_location: Optional[str] = None
    name_override: Optional[str] = None
    almost_finished: Optional[bool] = None

class RecipeIngredientInput(BaseModel):
    barcode: str
    quantity: float = 1.0
    unit: str = "unit"

class RecipeCreate(BaseModel):
    name: str
    time_type: TimeTypeEnum = TimeTypeEnum.MODERATE
    ingredients: List[RecipeIngredientInput] = []

class RecipeUpdate(BaseModel):
    name: Optional[str] = None
    time_type: Optional[TimeTypeEnum] = None
    ingredients: Optional[List[RecipeIngredientInput]] = None

# Initialize database
init_db()

app = FastAPI(title="Food Logger API", debug=DEBUG)

# Configure CORS
from fastapi.middleware.cors import CORSMiddleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Serve static files (frontend)
frontend_dir = Path(__file__).parent.parent / "frontend"
if frontend_dir.exists():
    app.mount("/static", StaticFiles(directory=frontend_dir), name="static")

# ============================================================================
# HEALTH CHECK
# ============================================================================

@app.get("/health")
def health_check():
    """Health check endpoint for Docker"""
    return {"status": "healthy"}


# ============================================================================
# PRODUCT ENDPOINTS
# ============================================================================

@app.post("/api/products")
def create_product(product: ProductCreate, db: Session = Depends(get_db)):
    """Create a new product"""
    # Check if product already exists
    existing = db.query(Product).filter(Product.barcode == product.barcode).first()
    if existing:
        raise HTTPException(status_code=400, detail="Product with this barcode already exists")

    db_product = Product(
        barcode=product.barcode,
        name=product.name,
        brand=product.brand,
        category=product.category,
        serving_size=product.serving_size,
        kcal=product.kcal,
        protein=product.protein,
        carbs=product.carbs,
        fat=product.fat,
    )
    db.add(db_product)
    db.commit()
    db.refresh(db_product)
    return product_to_dict(db_product)


@app.get("/api/products")
def list_products(
    search: str = Query(None),
    db: Session = Depends(get_db)
):
    """List all products with optional search"""
    query = db.query(Product)

    if search:
        query = query.filter(
            (Product.name.ilike(f"%{search}%")) |
            (Product.brand.ilike(f"%{search}%")) |
            (Product.barcode.ilike(f"%{search}%"))
        )

    products = query.order_by(Product.name).all()
    return [product_to_dict(p) for p in products]


@app.get("/api/products/{barcode}")
def get_product(barcode: str, db: Session = Depends(get_db)):
    """Get product by barcode from local cache, then Open Food Facts"""
    product = db.query(Product).filter(Product.barcode == barcode).first()
    if product:
        return {
            "status": "local",
            "product": product_to_dict(product),
            "message": "Product found locally",
        }

    off_product, off_error = fetch_product_from_open_food_facts(barcode)
    if off_product:
        db_product = Product(
            barcode=off_product["barcode"],
            name=off_product["name"],
            brand=off_product["brand"],
            category=off_product["category"],
            serving_size=off_product["serving_size"],
            kcal=off_product["kcal"],
            protein=off_product["protein"],
            carbs=off_product["carbs"],
            fat=off_product["fat"],
        )
        db.add(db_product)
        db.commit()
        db.refresh(db_product)
        return {
            "status": "off",
            "product": product_to_dict(db_product),
            "message": "Product fetched from Open Food Facts",
        }

    if off_error == "off_unavailable":
        return {
            "status": "off_unavailable",
            "product": None,
            "message": "Open Food Facts is unavailable right now",
        }

    return {
        "status": "not_found",
        "product": None,
        "message": "Product not found locally or in Open Food Facts",
    }


@app.patch("/api/products/{barcode}")
def update_product(barcode: str, product: ProductUpdate, db: Session = Depends(get_db)):
    """Update product by barcode"""
    db_product = db.query(Product).filter(Product.barcode == barcode).first()
    if not db_product:
        raise HTTPException(status_code=404, detail="Product not found")

    if product.name is not None:
        db_product.name = product.name
    if product.brand is not None:
        db_product.brand = product.brand
    if product.category is not None:
        db_product.category = product.category
    if product.serving_size is not None:
        db_product.serving_size = product.serving_size
    if product.kcal is not None:
        db_product.kcal = product.kcal
    if product.protein is not None:
        db_product.protein = product.protein
    if product.carbs is not None:
        db_product.carbs = product.carbs
    if product.fat is not None:
        db_product.fat = product.fat

    db_product.last_updated = datetime.utcnow()
    db.commit()
    db.refresh(db_product)
    return product_to_dict(db_product)


@app.delete("/api/products/{barcode}")
def delete_product(barcode: str, db: Session = Depends(get_db)):
    """Delete product by barcode (cascades to inventory and recipes)"""
    product = db.query(Product).filter(Product.barcode == barcode).first()
    if not product:
        raise HTTPException(status_code=404, detail="Product not found")

    db.delete(product)
    db.commit()
    return {"message": "Product deleted"}


# ============================================================================
# INVENTORY ENDPOINTS
# ============================================================================

@app.post("/api/inventory")
def create_inventory(inventory: InventoryCreate, db: Session = Depends(get_db)):
    """Create inventory item"""
    # Check if product exists
    product = db.query(Product).filter(Product.barcode == inventory.barcode).first()
    if not product:
        raise HTTPException(status_code=404, detail="Product not found")

    db_inventory = Inventory(
        barcode=inventory.barcode,
        quantity=inventory.quantity,
        unit=inventory.unit,
        date_bought=inventory.date_bought,
        expiry_date=inventory.expiry_date,
        storage_location=inventory.storage_location,
        name_override=inventory.name_override,
        almost_finished=inventory.almost_finished,
    )
    db.add(db_inventory)
    db.commit()
    db.refresh(db_inventory)
    return inventory_to_dict(db_inventory)


@app.get("/api/inventory")
def list_inventory(
    storage_location: str = Query(None),
    almost_finished: Optional[bool] = Query(None),
    sort_by: str = Query("expiry_date"),  # or "date_created", "storage_location"
    db: Session = Depends(get_db)
):
    """List inventory with optional filters"""
    query = db.query(Inventory).join(Product)

    if storage_location:
        query = query.filter(Inventory.storage_location == storage_location)
    if almost_finished is not None:
        query = query.filter(Inventory.almost_finished == almost_finished)

    # Sort
    if sort_by == "expiry_date":
        query = query.order_by(Inventory.expiry_date.asc().nullslast())
    elif sort_by == "date_created":
        query = query.order_by(Inventory.date_created.desc())
    elif sort_by == "storage_location":
        query = query.order_by(Inventory.storage_location.asc())

    items = query.all()
    return [inventory_to_dict(item) for item in items]


@app.get("/api/inventory/{item_id}")
def get_inventory_item(item_id: int, db: Session = Depends(get_db)):
    """Get single inventory item"""
    item = db.query(Inventory).filter(Inventory.id == item_id).first()
    if not item:
        raise HTTPException(status_code=404, detail="Inventory item not found")
    return inventory_to_dict(item)


@app.patch("/api/inventory/{item_id}")
def update_inventory_item(item_id: int, item: InventoryUpdate, db: Session = Depends(get_db)):
    """Update inventory item"""
    db_item = db.query(Inventory).filter(Inventory.id == item_id).first()
    if not db_item:
        raise HTTPException(status_code=404, detail="Inventory item not found")

    if item.quantity is not None:
        db_item.quantity = item.quantity
    if item.unit is not None:
        db_item.unit = item.unit
    if item.date_bought is not None:
        db_item.date_bought = item.date_bought
    if item.expiry_date is not None:
        db_item.expiry_date = item.expiry_date
    if item.storage_location is not None:
        db_item.storage_location = item.storage_location
    if item.name_override is not None:
        db_item.name_override = item.name_override
    if item.almost_finished is not None:
        db_item.almost_finished = item.almost_finished

    db.commit()
    db.refresh(db_item)
    return inventory_to_dict(db_item)


@app.delete("/api/inventory/{item_id}")
def delete_inventory_item(item_id: int, db: Session = Depends(get_db)):
    """Delete inventory item"""
    item = db.query(Inventory).filter(Inventory.id == item_id).first()
    if not item:
        raise HTTPException(status_code=404, detail="Inventory item not found")

    db.delete(item)
    db.commit()
    return {"message": "Inventory item deleted"}


# ============================================================================
# RECIPE ENDPOINTS
# ============================================================================

@app.post("/api/recipes")
def create_recipe(recipe: RecipeCreate, db: Session = Depends(get_db)):
    """Create recipe with ingredients"""
    db_recipe = Recipe(name=recipe.name, time_type=recipe.time_type)
    db.add(db_recipe)
    db.flush()  # Get recipe ID

    if recipe.ingredients:
        for ingredient in recipe.ingredients:
            # Verify product exists
            product = db.query(Product).filter(Product.barcode == ingredient.barcode).first()
            if not product:
                db.rollback()
                raise HTTPException(status_code=404, detail=f"Product {ingredient.barcode} not found")

            recipe_ingredient = RecipeIngredient(
                recipe_id=db_recipe.id,
                barcode=ingredient.barcode,
                quantity=ingredient.quantity,
                unit=ingredient.unit
            )
            db.add(recipe_ingredient)

    db.commit()
    db.refresh(db_recipe)
    return recipe_to_dict(db_recipe)


@app.get("/api/recipes")
def list_recipes(db: Session = Depends(get_db)):
    """List all recipes"""
    recipes = db.query(Recipe).order_by(Recipe.name).all()
    return [recipe_to_dict(recipe) for recipe in recipes]


@app.get("/api/recipes/{recipe_id}")
def get_recipe(recipe_id: int, db: Session = Depends(get_db)):
    """Get recipe with ingredients"""
    recipe = db.query(Recipe).filter(Recipe.id == recipe_id).first()
    if not recipe:
        raise HTTPException(status_code=404, detail="Recipe not found")
    return recipe_to_dict(recipe)


@app.patch("/api/recipes/{recipe_id}")
def update_recipe(recipe_id: int, recipe: RecipeUpdate, db: Session = Depends(get_db)):
    """Update recipe"""
    db_recipe = db.query(Recipe).filter(Recipe.id == recipe_id).first()
    if not db_recipe:
        raise HTTPException(status_code=404, detail="Recipe not found")

    if recipe.name is not None:
        db_recipe.name = recipe.name
    if recipe.time_type is not None:
        db_recipe.time_type = recipe.time_type

    if recipe.ingredients is not None:
        # Clear existing ingredients
        db.query(RecipeIngredient).filter(RecipeIngredient.recipe_id == recipe_id).delete()

        # Add new ingredients
        for ingredient in recipe.ingredients:
            product = db.query(Product).filter(Product.barcode == ingredient.barcode).first()
            if not product:
                db.rollback()
                raise HTTPException(status_code=404, detail=f"Product {ingredient.barcode} not found")

            recipe_ingredient = RecipeIngredient(
                recipe_id=recipe_id,
                barcode=ingredient.barcode,
                quantity=ingredient.quantity,
                unit=ingredient.unit
            )
            db.add(recipe_ingredient)

    db.commit()
    db.refresh(db_recipe)
    return recipe_to_dict(db_recipe)


@app.delete("/api/recipes/{recipe_id}")
def delete_recipe(recipe_id: int, db: Session = Depends(get_db)):
    """Delete recipe"""
    recipe = db.query(Recipe).filter(Recipe.id == recipe_id).first()
    if not recipe:
        raise HTTPException(status_code=404, detail="Recipe not found")

    db.delete(recipe)
    db.commit()
    return {"message": "Recipe deleted"}


# ============================================================================
# HELPER FUNCTIONS
# ============================================================================

def product_to_dict(product):
    """Convert Product to dictionary"""
    if not product:
        return None
    return {
        "barcode": product.barcode,
        "name": product.name,
        "brand": product.brand,
        "category": product.category,
        "serving_size": product.serving_size,
        "kcal": product.kcal,
        "protein": product.protein,
        "carbs": product.carbs,
        "fat": product.fat,
        "created_at": product.created_at.isoformat() if product.created_at else None,
        "last_updated": product.last_updated.isoformat() if product.last_updated else None,
    }


def inventory_to_dict(item):
    """Convert Inventory to dictionary"""
    if not item:
        return None
    return {
        "id": item.id,
        "barcode": item.barcode,
        "product_name": item.product.name if item.product else None,
        "quantity": item.quantity,
        "unit": item.unit,
        "date_bought": item.date_bought.isoformat() if item.date_bought else None,
        "expiry_date": item.expiry_date.isoformat() if item.expiry_date else None,
        "storage_location": item.storage_location,
        "name_override": item.name_override,
        "almost_finished": item.almost_finished,
        "date_created": item.date_created.isoformat() if item.date_created else None,
    }


def recipe_to_dict(recipe):
    """Convert Recipe to dictionary"""
    if not recipe:
        return None
    return {
        "id": recipe.id,
        "name": recipe.name,
        "time_type": recipe.time_type.value if recipe.time_type else None,
        "created_at": recipe.created_at.isoformat() if recipe.created_at else None,
        "ingredients": [
            {
                "barcode": ing.barcode,
                "product_name": ing.product.name if ing.product else None,
                "quantity": ing.quantity,
                "unit": ing.unit,
            }
            for ing in recipe.ingredients
        ],
    }


# Serve index.html for root path
@app.get("/")
def serve_root():
    """Serve index.html"""
    index_path = frontend_dir / "index.html"
    if index_path.exists():
        return FileResponse(index_path)
    return {"message": "Food Logger API ready"}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host=HOST, port=PORT)
