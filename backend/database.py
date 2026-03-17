from datetime import datetime
from enum import Enum
from sqlalchemy import create_engine, Column, Integer, String, Text, DateTime, Float, ForeignKey, Enum as SQLEnum, Index, Boolean, inspect, text
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker, relationship
import sys
from pathlib import Path

# Add parent directory to path to import config
sys.path.insert(0, str(Path(__file__).parent))
from config import DATABASE_URL

engine = create_engine(
    DATABASE_URL,
    connect_args={"check_same_thread": False},
    echo=False,
)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

Base = declarative_base()


class TimeTypeEnum(str, Enum):
    """Enum for recipe preparation time types"""
    VERY_FAST = "very_fast"
    MODERATE = "moderate"
    SLOW = "slow"


class Product(Base):
    """Global product entity keyed by barcode"""
    __tablename__ = "products"

    barcode = Column(String(50), primary_key=True, nullable=False)
    name = Column(String(255), nullable=False)
    brand = Column(String(255), nullable=True)
    category = Column(String(100), nullable=True)
    serving_size = Column(String(50), nullable=True)  # e.g., "100g", "1 cup"
    kcal = Column(Float, nullable=True)
    protein = Column(Float, nullable=True)  # grams
    carbs = Column(Float, nullable=True)  # grams
    fat = Column(Float, nullable=True)  # grams
    created_at = Column(DateTime, default=datetime.utcnow)
    last_updated = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

    # Relationships
    inventory_items = relationship("Inventory", back_populates="product")
    recipe_ingredients = relationship("RecipeIngredient", back_populates="product")

    __table_args__ = (
        Index('ix_products_barcode', 'barcode'),
    )


class Inventory(Base):
    """User inventory items"""
    __tablename__ = "inventory"

    id = Column(Integer, primary_key=True)
    barcode = Column(String(50), ForeignKey("products.barcode"), nullable=False)
    quantity = Column(Float, nullable=False, default=1.0)
    unit = Column(String(50), default="unit")  # e.g., "kg", "unit", "liter"
    date_bought = Column(DateTime, nullable=True)
    expiry_date = Column(DateTime, nullable=True)
    storage_location = Column(String(100), nullable=True)  # e.g., "fridge", "freezer", "pantry"
    name_override = Column(String(255), nullable=True)  # User-supplied simplified name
    almost_finished = Column(Boolean, nullable=False, default=False)
    date_created = Column(DateTime, default=datetime.utcnow)

    # Relationships
    product = relationship("Product", back_populates="inventory_items")

    __table_args__ = (
        Index('ix_inventory_expiry', 'expiry_date'),
        Index('ix_inventory_barcode', 'barcode'),
    )


class Recipe(Base):
    """Recipe entity"""
    __tablename__ = "recipes"

    id = Column(Integer, primary_key=True)
    name = Column(String(255), nullable=False)
    time_type = Column(SQLEnum(TimeTypeEnum), default=TimeTypeEnum.MODERATE)
    created_at = Column(DateTime, default=datetime.utcnow)

    # Relationships
    ingredients = relationship("RecipeIngredient", back_populates="recipe", cascade="all, delete-orphan")


class RecipeIngredient(Base):
    """Many-to-many linking of recipes and products"""
    __tablename__ = "recipe_ingredients"

    id = Column(Integer, primary_key=True)
    recipe_id = Column(Integer, ForeignKey("recipes.id"), nullable=False)
    barcode = Column(String(50), ForeignKey("products.barcode"), nullable=False)
    quantity = Column(Float, nullable=False, default=1.0)
    unit = Column(String(50), default="unit")

    # Relationships
    recipe = relationship("Recipe", back_populates="ingredients")
    product = relationship("Product", back_populates="recipe_ingredients")


def get_db():
    """Dependency for getting database session"""
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


def init_db():
    """Initialize database tables"""
    Base.metadata.create_all(bind=engine)

    # Lightweight migration for existing SQLite files created before almost_finished existed.
    inspector = inspect(engine)
    inventory_columns = [col["name"] for col in inspector.get_columns("inventory")]
    if "almost_finished" not in inventory_columns:
        with engine.begin() as conn:
            conn.execute(text("ALTER TABLE inventory ADD COLUMN almost_finished BOOLEAN NOT NULL DEFAULT 0"))
