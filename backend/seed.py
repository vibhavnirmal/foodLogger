"""
Seed script to populate test data in the database
Run with: python seed.py
"""
import sys
from pathlib import Path
from datetime import datetime, timedelta

sys.path.insert(0, str(Path(__file__).parent))

from database import SessionLocal, init_db, Product, Inventory, Recipe, RecipeIngredient, TimeTypeEnum

def seed_data():
    """Create test data"""
    init_db()
    db = SessionLocal()

    # Clear existing data
    db.query(RecipeIngredient).delete()
    db.query(Recipe).delete()
    db.query(Inventory).delete()
    db.query(Product).delete()
    db.commit()

    # Create test products
    products = [
        Product(
            barcode="5901234123456",
            name="Whole Milk",
            brand="Local Dairy",
            category="Dairy",
            serving_size="200ml",
            kcal=60,
            protein=3.2,
            carbs=4.8,
            fat=3.2,
        ),
        Product(
            barcode="5901234123457",
            name="Eggs",
            brand="Farm Fresh",
            category="Dairy",
            serving_size="1 large",
            kcal=78,
            protein=6.3,
            carbs=0.6,
            fat=5.3,
        ),
        Product(
            barcode="5901234123458",
            name="All-Purpose Flour",
            brand="King Arthur",
            category="Baking",
            serving_size="30g",
            kcal=110,
            protein=3.0,
            carbs=23.0,
            fat=0.0,
        ),
        Product(
            barcode="5901234123459",
            name="Granulated Sugar",
            brand="Generic",
            category="Baking",
            serving_size="1 tsp",
            kcal=16,
            protein=0.0,
            carbs=4.0,
            fat=0.0,
        ),
        Product(
            barcode="5901234123460",
            name="Pasta (Spaghetti)",
            brand="De Cecco",
            category="Grains",
            serving_size="100g dry",
            kcal=371,
            protein=13.0,
            carbs=75.0,
            fat=1.1,
        ),
        Product(
            barcode="5901234123461",
            name="Tomato Sauce",
            brand="San Marzano",
            category="Condiments",
            serving_size="100ml",
            kcal=28,
            protein=1.2,
            carbs=5.0,
            fat=0.1,
        ),
        Product(
            barcode="5901234123462",
            name="Butter",
            brand="Normandy",
            category="Dairy",
            serving_size="14g",
            kcal=102,
            protein=0.1,
            carbs=0.0,
            fat=11.5,
        ),
        Product(
            barcode="5901234123463",
            name="Olive Oil",
            brand="Extra Virgin",
            category="Oils",
            serving_size="15ml",
            kcal=120,
            protein=0.0,
            carbs=0.0,
            fat=14.0,
        ),
    ]

    db.add_all(products)
    db.commit()

    # Create test inventory items
    now = datetime.utcnow()
    inventory_items = [
        Inventory(
            barcode="5901234123456",
            quantity=2.0,
            unit="liter",
            date_bought=now - timedelta(days=3),
            expiry_date=now + timedelta(days=11),
            storage_location="fridge",
            name_override=None,
        ),
        Inventory(
            barcode="5901234123457",
            quantity=6.0,
            unit="unit",
            date_bought=now - timedelta(days=5),
            expiry_date=now + timedelta(days=30),
            storage_location="fridge",
            name_override=None,
        ),
        Inventory(
            barcode="5901234123458",
            quantity=1.0,
            unit="kg",
            date_bought=now - timedelta(days=30),
            expiry_date=now + timedelta(days=90),
            storage_location="pantry",
            name_override=None,
        ),
        Inventory(
            barcode="5901234123460",
            quantity=500.0,
            unit="g",
            date_bought=now - timedelta(days=60),
            expiry_date=now + timedelta(days=300),
            storage_location="pantry",
            name_override=None,
        ),
        Inventory(
            barcode="5901234123461",
            quantity=680.0,
            unit="ml",
            date_bought=now - timedelta(days=2),
            expiry_date=now + timedelta(days=60),
            storage_location="pantry",
            name_override=None,
        ),
    ]

    db.add_all(inventory_items)
    db.commit()

    # Create test recipes
    recipe1 = Recipe(name="Pancakes", time_type=TimeTypeEnum.MODERATE)
    recipe1.ingredients = [
        RecipeIngredient(barcode="5901234123458", quantity=2.0, unit="cups"),
        RecipeIngredient(barcode="5901234123457", quantity=2.0, unit="unit"),
        RecipeIngredient(barcode="5901234123456", quantity=1.5, unit="cups"),
        RecipeIngredient(barcode="5901234123459", quantity=2.0, unit="tbsp"),
        RecipeIngredient(barcode="5901234123462", quantity=2.0, unit="tbsp"),
    ]

    recipe2 = Recipe(name="Spaghetti Marinara", time_type=TimeTypeEnum.VERY_FAST)
    recipe2.ingredients = [
        RecipeIngredient(barcode="5901234123460", quantity=400.0, unit="g"),
        RecipeIngredient(barcode="5901234123461", quantity=500.0, unit="ml"),
        RecipeIngredient(barcode="5901234123463", quantity=3.0, unit="tbsp"),
    ]

    db.add_all([recipe1, recipe2])
    db.commit()

    print("✓ Database seeded with test data")
    print(f"  - {len(products)} products")
    print(f"  - {len(inventory_items)} inventory items")
    print(f"  - 2 recipes")


if __name__ == "__main__":
    seed_data()
