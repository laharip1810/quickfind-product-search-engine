package com.quickfind.seed;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Hand-written demo catalog: 114 products across 10 subcategories, plus demo users and a
 * small set of interactions, so search, trending and co-interaction recommendations work
 * on first start. Prices are in INR.
 *
 * <p>Pure data: no Spring, no JPA. That lets a unit test check it (unique names, every
 * brand/color/category reference resolves) without starting the application.
 */
public final class CatalogSeedData {

    public static final String RUNNING_SHOES = "Running Shoes";
    public static final String SNEAKERS = "Sneakers";
    public static final String T_SHIRTS = "T-Shirts";
    public static final String JEANS = "Jeans";
    public static final String JACKETS = "Jackets";
    public static final String SHORTS = "Shorts";
    public static final String TRACK_PANTS = "Track Pants";
    public static final String WATCHES = "Watches";
    public static final String BAGS = "Bags";
    public static final String SPORTS_ACCESSORIES = "Sports Accessories";

    public static final String DEMO_USER_EMAIL = "demo@quickfind.dev";

    public record SeedProduct(String brand, String name, String subcategory, int price, int discountPercent,
                              double rating, int reviewCount, int stock, String description,
                              List<String> colors, List<String> sizes) {
    }

    public record SeedUser(String email, String displayName, String role) {
    }

    /** productName is the full product name (brand included); null for SEARCH events. */
    public record SeedInteraction(String userEmail, String productName, String type, String query, int hoursAgo) {
    }

    /** Top-level category to its subcategories. */
    public static final Map<String, List<String>> CATEGORIES = Map.of(
            "Footwear", List.of(RUNNING_SHOES, SNEAKERS),
            "Clothing", List.of(T_SHIRTS, JEANS, JACKETS, SHORTS, TRACK_PANTS),
            "Accessories", List.of(WATCHES, BAGS, SPORTS_ACCESSORIES));

    public static final List<String> BRANDS = List.of(
            "Nike", "Adidas", "Puma", "ASICS", "New Balance", "Skechers", "Reebok", "Campus", "Converse",
            "Vans", "Red Tape", "Under Armour", "H&M", "Uniqlo", "Allen Solly", "Tommy Hilfiger",
            "Decathlon", "Boldfit", "Roadster", "Levi's", "Wrangler", "The North Face", "Columbia",
            "Wildcraft", "Titan", "Fossil", "Casio", "Fastrack", "Garmin", "Noise", "American Tourister",
            "Skybags");

    /** Color name to hex code (used for swatches in the UI). */
    public static final Map<String, String> COLORS = Map.ofEntries(
            Map.entry("Black", "#111827"), Map.entry("White", "#F3F4F6"), Map.entry("Grey", "#9CA3AF"),
            Map.entry("Navy", "#1E3A8A"), Map.entry("Blue", "#2563EB"), Map.entry("Red", "#DC2626"),
            Map.entry("Green", "#16A34A"), Map.entry("Olive", "#6B7A3A"), Map.entry("Beige", "#D6C7A1"),
            Map.entry("Brown", "#7C4A2D"), Map.entry("Pink", "#EC4899"), Map.entry("Yellow", "#EAB308"),
            Map.entry("Orange", "#F97316"), Map.entry("Maroon", "#7F1D1D"), Map.entry("Silver", "#C0C4CC"),
            Map.entry("Gold", "#C9A227"));

    public static final List<SeedUser> USERS = List.of(
            new SeedUser(DEMO_USER_EMAIL, "Demo Shopper", "CUSTOMER"),
            new SeedUser("admin@quickfind.dev", "Store Admin", "ADMIN"),
            new SeedUser("aarav@quickfind.dev", "Aarav", "CUSTOMER"),
            new SeedUser("diya@quickfind.dev", "Diya", "CUSTOMER"),
            new SeedUser("kabir@quickfind.dev", "Kabir", "CUSTOMER"),
            new SeedUser("meera@quickfind.dev", "Meera", "CUSTOMER"),
            new SeedUser("rohan@quickfind.dev", "Rohan", "CUSTOMER"),
            new SeedUser("sara@quickfind.dev", "Sara", "CUSTOMER"));

    private static final Map<String, String> SUBCATEGORY_DESCRIPTIONS = Map.of(
            RUNNING_SHOES, "Designed for road and treadmill running, with a secure lace-up fit and a durable rubber outsole.",
            SNEAKERS, "An everyday sneaker with a cushioned footbed and a versatile look that pairs with jeans or joggers.",
            T_SHIRTS, "Soft, breathable fabric with a regular fit that works for training days and casual wear.",
            JEANS, "Classic five-pocket denim with reinforced stitching and a comfortable waistband.",
            JACKETS, "A layering piece with zip pockets that handles changing weather on commutes and trails.",
            SHORTS, "Lightweight shorts with an elastic waistband and quick-dry fabric for workouts and warm days.",
            TRACK_PANTS, "Tapered track pants with a drawcord waist and zip pockets for training and travel.",
            WATCHES, "A durable case and a comfortable strap make it easy to wear from morning to night.",
            BAGS, "Organised compartments and padded straps keep your essentials comfortable to carry all day.",
            SPORTS_ACCESSORIES, "Built to support your training routine and made to last through regular use.");

    private static final List<String> SHOE_SIZES = List.of("UK 6", "UK 7", "UK 8", "UK 9", "UK 10", "UK 11");
    private static final List<String> APPAREL_SIZES = List.of("S", "M", "L", "XL", "XXL");
    private static final List<String> WAIST_SIZES = List.of("28", "30", "32", "34", "36");
    private static final List<String> SOCK_SIZES = List.of("S", "M", "L");
    private static final List<String> ONE_SIZE = List.of("One Size");

    private CatalogSeedData() {
    }

    public static List<String> defaultSizes(String subcategory) {
        return switch (subcategory) {
            case RUNNING_SHOES, SNEAKERS -> SHOE_SIZES;
            case T_SHIRTS, JACKETS, SHORTS, TRACK_PANTS -> APPAREL_SIZES;
            case JEANS -> WAIST_SIZES;
            default -> ONE_SIZE;
        };
    }

    public static String subcategoryDescription(String subcategory) {
        return SUBCATEGORY_DESCRIPTIONS.getOrDefault(subcategory, "");
    }

    /** Popularity derived from rating and review volume: rating · log10(reviews + 10) · 5. */
    public static double popularity(double rating, int reviewCount) {
        return Math.round(rating * Math.log10(reviewCount + 10) * 5 * 100.0) / 100.0;
    }

    public static List<SeedProduct> products() {
        List<SeedProduct> p = new ArrayList<>();

        // ---- Running Shoes (16) ----
        p.add(product("Nike", "Pegasus 41 Running Shoes", RUNNING_SHOES, 11895, 10, 4.6, 2140, 45,
                "Responsive ReactX foam and an engineered mesh upper make it a reliable daily trainer", "Black", "White", "Blue"));
        p.add(product("Nike", "Invincible 3 Running Shoes", RUNNING_SHOES, 16995, 15, 4.5, 860, 20,
                "Max-cushion ZoomX foam softens every stride on long recovery runs", "Black", "Grey"));
        p.add(product("Nike", "Revolution 7 Running Shoes", RUNNING_SHOES, 3695, 5, 4.2, 5230, 120,
                "A lightweight, budget-friendly shoe with soft foam for new runners", "Black", "White", "Navy"));
        p.add(product("Adidas", "Ultraboost Light Running Shoes", RUNNING_SHOES, 18999, 20, 4.6, 1420, 30,
                "Light Boost cushioning returns energy with every step", "Black", "White"));
        p.add(product("Adidas", "Adizero SL Running Shoes", RUNNING_SHOES, 9999, 10, 4.4, 640, 35,
                "Lightstrike Pro foam in the forefoot adds speed for tempo runs", "Blue", "White"));
        p.add(product("Adidas", "Duramo SL Running Shoes", RUNNING_SHOES, 4599, 25, 4.1, 3120, 90,
                "A breathable mesh upper and Lightmotion cushioning for everyday miles", "Black", "Grey", "Navy"));
        p.add(product("ASICS", "Gel-Kayano 31 Running Shoes", RUNNING_SHOES, 16999, 10, 4.7, 980, 18,
                "Stability-focused support with GEL cushioning for runners who overpronate", "Black", "Blue"));
        p.add(product("ASICS", "Gel-Nimbus 26 Running Shoes", RUNNING_SHOES, 15999, 12, 4.6, 720, 0,
                "Plush FF BLAST+ foam for maximum comfort on long runs", "Grey", "White"));
        p.add(product("ASICS", "Gel-Contend 8 Running Shoes", RUNNING_SHOES, 4499, 20, 4.3, 2890, 75,
                "Rearfoot GEL technology absorbs shock on easy runs", "Black", "Navy"));
        p.add(product("New Balance", "Fresh Foam X 1080v13 Running Shoes", RUNNING_SHOES, 16999, 10, 4.6, 510, 14,
                "Fresh Foam X cushioning with a roomy toe box for long distances", "Black", "White"));
        p.add(product("New Balance", "FuelCell Rebel v4 Running Shoes", RUNNING_SHOES, 12999, 15, 4.5, 430, 22,
                "Bouncy FuelCell foam for fast, playful training sessions", "Orange", "White"));
        p.add(product("Puma", "Velocity Nitro 3 Running Shoes", RUNNING_SHOES, 11999, 30, 4.4, 690, 40,
                "Nitrogen-infused NITRO foam with PUMAGRIP rubber for wet roads", "Black", "Red"));
        p.add(product("Puma", "Softride Pro Running Shoes", RUNNING_SHOES, 4999, 40, 4.0, 1850, 60,
                "A SoftFoam+ sockliner adds step-in comfort for daily jogs", "Black", "Grey"));
        p.add(product("Skechers", "GOrun Consistent Running Shoes", RUNNING_SHOES, 5499, 20, 4.2, 1240, 55,
                "An Air-Cooled Goga Mat insole and lightweight ULTRA GO cushioning", "Black", "Navy"));
        p.add(product("Reebok", "Floatride Energy 5 Running Shoes", RUNNING_SHOES, 9999, 35, 4.3, 560, 0,
                "Floatride Energy Foam balances cushioning and responsiveness", "Black", "Blue"));
        p.add(product("Campus", "North Plus Running Shoes", RUNNING_SHOES, 1899, 10, 4.0, 8420, 200,
                "An affordable lace-up runner with a cushioned EVA midsole", "Black", "Grey", "Blue"));

        // ---- Sneakers (14) ----
        p.add(product("Nike", "Air Force 1 '07 Sneakers", SNEAKERS, 9695, 0, 4.7, 6120, 80,
                "The iconic low-top with a stitched leather upper and Air cushioning", "White", "Black"));
        p.add(product("Nike", "Dunk Low Retro Sneakers", SNEAKERS, 8695, 0, 4.6, 2310, 25,
                "Basketball heritage with a padded low-cut collar and leather overlays", "White", "Black", "Grey"));
        p.add(product("Adidas", "Samba OG Sneakers", SNEAKERS, 10999, 0, 4.7, 3420, 15,
                "A football-inspired classic with a suede T-toe and a gum rubber sole", "White", "Black"));
        p.add(product("Adidas", "Stan Smith Sneakers", SNEAKERS, 8999, 10, 4.5, 4100, 50,
                "A clean leather upper with perforated 3-Stripes", "White", "Green"));
        p.add(product("Puma", "Suede Classic XXI Sneakers", SNEAKERS, 6999, 30, 4.4, 2800, 70,
                "A soft suede upper with the formstrip that started it all", "Black", "Navy", "Red"));
        p.add(product("Puma", "Carina 2.0 Sneakers", SNEAKERS, 4999, 45, 4.2, 1960, 65,
                "A platform sole and SoftFoam+ comfort for everyday style", "White", "Pink"));
        p.add(product("New Balance", "574 Core Sneakers", SNEAKERS, 8999, 10, 4.5, 1720, 40,
                "ENCAP midsole support in a suede-and-mesh retro silhouette", "Grey", "Navy"));
        p.add(product("New Balance", "550 Sneakers", SNEAKERS, 11999, 0, 4.6, 1340, 12,
                "An 80s basketball-inspired leather sneaker with a perforated toe", "White", "Green"));
        p.add(product("Converse", "Chuck Taylor All Star High Top Sneakers", SNEAKERS, 4499, 0, 4.6, 7800, 150,
                "A canvas high-top with a vulcanised rubber sole", "Black", "White", "Red"));
        p.add(product("Vans", "Old Skool Sneakers", SNEAKERS, 5999, 10, 4.6, 5200, 90,
                "A suede and canvas skate shoe with the side stripe and waffle outsole", "Black", "White"));
        p.add(product("Skechers", "Uno Sneakers", SNEAKERS, 7999, 20, 4.4, 1500, 35,
                "Visible Skech-Air cushioning in a smooth synthetic upper", "White", "Black"));
        p.add(product("Reebok", "Club C 85 Sneakers", SNEAKERS, 6999, 40, 4.4, 2100, 45,
                "A soft garment-leather upper with a low-cut tennis silhouette", "White", "Green"));
        p.add(product("Campus", "OG-01 Sneakers", SNEAKERS, 1999, 15, 4.1, 6700, 180,
                "A lightweight everyday sneaker with a padded collar", "White", "Black", "Grey"));
        p.add(product("Red Tape", "Casual Leather Sneakers", SNEAKERS, 3299, 60, 4.0, 3900, 0,
                "A genuine leather upper with a cushioned insole", "White", "Brown"));

        // ---- T-Shirts (14) ----
        p.add(product("Nike", "Dri-FIT Running T-Shirt", T_SHIRTS, 2295, 10, 4.5, 1860, 140,
                "Sweat-wicking Dri-FIT fabric keeps you dry on hard efforts", "Black", "Blue", "Grey"));
        p.add(product("Nike", "Sportswear Club Cotton T-Shirt", T_SHIRTS, 1795, 0, 4.4, 3200, 200,
                "Soft cotton jersey with the embroidered Swoosh", "Black", "White", "Navy"));
        p.add(product("Adidas", "Own The Run T-Shirt", T_SHIRTS, 2299, 30, 4.4, 940, 110,
                "AEROREADY moisture management and reflective details for evening runs", "Black", "Red"));
        p.add(product("Adidas", "Essentials 3-Stripes T-Shirt", T_SHIRTS, 1699, 20, 4.3, 2500, 160,
                "A classic cotton tee with the 3-Stripes on the shoulders", "White", "Black", "Navy"));
        p.add(product("Puma", "Run Favourite Graphic Sports T-Shirt", T_SHIRTS, 1999, 40, 4.2, 780, 95,
                "dryCELL technology and a lightweight feel for runs and workouts", "Blue", "Black"));
        p.add(product("Under Armour", "Tech 2.0 Training T-Shirt", T_SHIRTS, 1999, 25, 4.5, 2300, 130,
                "UA Tech fabric is quick-drying, ultra-soft and has a natural feel", "Black", "Grey", "Red"));
        p.add(product("Under Armour", "Sportstyle Logo T-Shirt", T_SHIRTS, 1799, 20, 4.3, 900, 85,
                "Charged Cotton that dries faster than regular cotton", "Navy", "White"));
        p.add(product("H&M", "Regular Fit Crew-neck T-Shirt", T_SHIRTS, 499, 0, 4.1, 11200, 300,
                "A cotton jersey basic in a regular fit", "White", "Black", "Beige", "Olive"));
        p.add(product("Uniqlo", "AIRism Cotton Oversized T-Shirt", T_SHIRTS, 1490, 0, 4.5, 4300, 180,
                "AIRism lining keeps the smooth cotton look while feeling cool", "White", "Black", "Olive"));
        p.add(product("Allen Solly", "Solid Polo T-Shirt", T_SHIRTS, 1299, 35, 4.2, 3700, 120,
                "Pique cotton polo with a ribbed collar for smart-casual days", "Navy", "Maroon", "White"));
        p.add(product("Tommy Hilfiger", "Slim Fit Flag Logo T-Shirt", T_SHIRTS, 2999, 30, 4.4, 850, 60,
                "Organic cotton with the signature flag logo", "White", "Navy"));
        p.add(product("Decathlon", "Kalenji Dry Running T-Shirt", T_SHIRTS, 499, 0, 4.3, 8900, 250,
                "Breathable mesh fabric for comfortable running in warm weather", "Blue", "Black", "Yellow"));
        p.add(product("Boldfit", "Gym Training T-Shirt", T_SHIRTS, 599, 50, 4.0, 5200, 220,
                "Four-way stretch fabric for lifting and HIIT sessions", "Black", "Grey", "Maroon"));
        p.add(product("Roadster", "Striped Cotton T-Shirt", T_SHIRTS, 699, 55, 3.9, 6100, 0,
                "A relaxed striped tee in breathable cotton", "Navy", "White"));

        // ---- Jeans (10) ----
        p.add(product("Levi's", "511 Slim Fit Jeans", JEANS, 3599, 30, 4.4, 4200, 90,
                "A modern slim fit that sits below the waist with room to move", "Blue", "Black"));
        p.add(product("Levi's", "501 Original Fit Jeans", JEANS, 4299, 20, 4.5, 2600, 60,
                "The original button-fly straight leg", "Blue"));
        p.add(product("Levi's", "541 Athletic Taper Jeans", JEANS, 3999, 35, 4.3, 1100, 45,
                "Extra room in the seat and thigh with a tapered leg", "Blue", "Grey"));
        p.add(product("Wrangler", "Regular Fit Stretch Jeans", JEANS, 2499, 40, 4.1, 1900, 70,
                "Comfort stretch denim in a regular fit", "Blue", "Black"));
        p.add(product("Wrangler", "Texas Straight Fit Jeans", JEANS, 2799, 30, 4.2, 800, 0,
                "Heritage straight-leg denim with a classic five-pocket build", "Blue"));
        p.add(product("H&M", "Slim Jeans", JEANS, 1999, 0, 4.0, 5300, 150,
                "Washed cotton denim with a touch of stretch", "Blue", "Black"));
        p.add(product("Uniqlo", "Ultra Stretch Skinny Jeans", JEANS, 2990, 0, 4.4, 2200, 100,
                "Four-way stretch denim that moves with you", "Blue", "Black", "Grey"));
        p.add(product("Roadster", "Mid-Rise Skinny Fit Jeans", JEANS, 1899, 60, 3.9, 7400, 180,
                "Clean-look skinny denim with light fading", "Blue", "Black"));
        p.add(product("Tommy Hilfiger", "Denton Straight Fit Jeans", JEANS, 6999, 30, 4.4, 420, 25,
                "Recycled-cotton denim in a straight fit", "Blue"));
        p.add(product("Allen Solly", "Tapered Fit Jeans", JEANS, 2299, 40, 4.0, 1300, 55,
                "Soft stretch denim with a tapered leg", "Blue", "Black"));

        // ---- Jackets (10) ----
        p.add(product("The North Face", "1996 Retro Nuptse Puffer Jacket", JACKETS, 32999, 0, 4.8, 640, 12,
                "700-fill goose down in the boxy 1996 fit", "Black", "Yellow"));
        p.add(product("The North Face", "Quest Waterproof Jacket", JACKETS, 11999, 20, 4.5, 900, 30,
                "A DryVent waterproof, breathable shell with an adjustable hood", "Black", "Navy", "Red"));
        p.add(product("Columbia", "Watertight II Rain Jacket", JACKETS, 7999, 30, 4.4, 1300, 40,
                "Omni-Tech waterproof protection that packs into its own pocket", "Black", "Blue"));
        p.add(product("Columbia", "Steens Mountain Fleece Jacket", JACKETS, 4999, 25, 4.5, 1500, 50,
                "Soft MTR filament fleece for warmth on cool days", "Grey", "Navy"));
        p.add(product("Nike", "Windrunner Running Jacket", JACKETS, 7495, 15, 4.4, 780, 35,
                "Water-repellent woven fabric with the classic chevron design", "Black", "Blue"));
        p.add(product("Adidas", "Own The Run Running Jacket", JACKETS, 5999, 35, 4.3, 610, 28,
                "Lightweight wind protection with reflective details for low-light runs", "Black", "Grey"));
        p.add(product("Puma", "Essentials Padded Jacket", JACKETS, 6999, 45, 4.1, 540, 0,
                "Recycled polyester padding for everyday warmth", "Black", "Olive"));
        p.add(product("Wildcraft", "Hypadry Rain Jacket", JACKETS, 2999, 30, 4.2, 2600, 80,
                "A seam-sealed waterproof shell built for the monsoon", "Blue", "Red", "Black"));
        p.add(product("Levi's", "Trucker Denim Jacket", JACKETS, 5999, 30, 4.6, 1900, 40,
                "The iconic denim jacket with button chest pockets", "Blue", "Black"));
        p.add(product("Decathlon", "Quechua MH500 Hiking Jacket", JACKETS, 4999, 0, 4.5, 3200, 60,
                "A warm, water-repellent jacket for mountain hikes", "Olive", "Navy"));

        // ---- Shorts (8) ----
        p.add(product("Nike", "Challenger 7-inch Running Shorts", SHORTS, 2995, 20, 4.5, 1400, 90,
                "Dri-FIT fabric with a brief liner and a zip pocket", "Black", "Blue"));
        p.add(product("Adidas", "Own The Run Running Shorts", SHORTS, 2499, 35, 4.3, 800, 75,
                "AEROREADY shorts with a built-in brief", "Black", "Grey"));
        p.add(product("Puma", "Performance Woven Running Shorts", SHORTS, 1999, 40, 4.2, 650, 60,
                "Lightweight woven fabric with dryCELL moisture control", "Black", "Navy"));
        p.add(product("ASICS", "Road 7-inch Running Shorts", SHORTS, 2799, 25, 4.4, 420, 35,
                "Breathable ventilation panels and a secure key pocket", "Black", "Orange"));
        p.add(product("Under Armour", "Launch 5-inch Running Shorts", SHORTS, 2999, 25, 4.5, 510, 40,
                "Stretch-woven fabric with a lightweight liner", "Black", "Red"));
        p.add(product("Decathlon", "Kalenji Dry Running Shorts", SHORTS, 599, 0, 4.3, 6100, 240,
                "Breathable shorts with an elastic waist for easy runs", "Black", "Blue"));
        p.add(product("Boldfit", "Gym Shorts with Zip Pockets", SHORTS, 699, 50, 4.0, 4300, 200,
                "Quick-dry fabric for workouts and lifting", "Black", "Grey", "Navy"));
        p.add(product("H&M", "Relaxed Fit Cotton Shorts", SHORTS, 1299, 0, 4.0, 900, 0,
                "Soft cotton twill shorts for warm days", "Beige", "Olive"));

        // ---- Track Pants (8) ----
        p.add(product("Nike", "Dri-FIT Challenger Running Track Pants", TRACK_PANTS, 4295, 20, 4.4, 520, 45,
                "Sweat-wicking knit with zip ankles for easy on and off", "Black"));
        p.add(product("Adidas", "Essentials 3-Stripes Track Pants", TRACK_PANTS, 3299, 40, 4.4, 2800, 110,
                "Tapered cotton-blend pants with side stripes", "Black", "Navy"));
        p.add(product("Adidas", "Tiro 24 Training Track Pants", TRACK_PANTS, 3999, 25, 4.5, 1600, 70,
                "Slim, zip-ankle training pants with AEROREADY", "Black", "Navy", "Red"));
        p.add(product("Puma", "Evostripe Track Pants", TRACK_PANTS, 3499, 50, 4.1, 700, 60,
                "dryCELL fabric with contrast side stripes", "Black", "Grey"));
        p.add(product("Under Armour", "Sportstyle Tricot Track Pants", TRACK_PANTS, 3999, 30, 4.3, 600, 0,
                "Smooth tricot fabric with a relaxed fit", "Black", "Navy"));
        p.add(product("Decathlon", "Domyos Slim Fit Track Pants", TRACK_PANTS, 799, 0, 4.2, 5500, 220,
                "Stretch fabric track pants for gym and yoga", "Black", "Grey"));
        p.add(product("Boldfit", "Reflective Running Track Pants", TRACK_PANTS, 899, 55, 4.0, 3100, 150,
                "Lightweight running track pants with reflective piping", "Black", "Navy"));
        p.add(product("Reebok", "Workout Ready Track Pants", TRACK_PANTS, 2999, 45, 4.1, 480, 50,
                "Speedwick fabric pulls sweat away for a dry feel", "Black", "Grey"));

        // ---- Watches (12) ----
        p.add(product("Titan", "Neo Analog Watch", WATCHES, 4995, 10, 4.4, 2100, 60,
                "A mineral glass dial with a stainless steel case", "Silver", "Black", "Gold"));
        p.add(product("Titan", "Edge Slim Analog Watch", WATCHES, 17995, 5, 4.6, 380, 15,
                "One of the slimmest watches, with a 4.4 mm case", "Silver"));
        p.add(product("Fossil", "Grant Chronograph Leather Watch", WATCHES, 12995, 30, 4.5, 1600, 35,
                "A Roman-numeral chronograph dial on a brown leather strap", "Brown", "Black"));
        p.add(product("Fossil", "Gen 6 Smart Watch", WATCHES, 22995, 40, 4.2, 900, 20,
                "A Wear OS smartwatch with SpO2, heart-rate tracking and fast charging", "Black", "Silver"));
        p.add(product("Casio", "G-Shock GA-2100 Digital Watch", WATCHES, 9995, 0, 4.7, 3400, 45,
                "Carbon Core Guard structure with 200 m water resistance", "Black", "Olive"));
        p.add(product("Casio", "Vintage A158WA Digital Watch", WATCHES, 1995, 0, 4.5, 8900, 120,
                "A retro stainless steel digital watch with an LED light", "Silver", "Gold"));
        p.add(product("Casio", "Edifice Chronograph Watch", WATCHES, 12995, 15, 4.5, 700, 25,
                "A stainless steel chronograph with a sporty tachymeter bezel", "Silver", "Black", "Blue"));
        p.add(product("Fastrack", "Reflex Play Smart Watch", WATCHES, 3995, 50, 4.0, 5200, 90,
                "An AMOLED display with heart-rate and sleep tracking", "Black", "Blue"));
        p.add(product("Fastrack", "Minimalist Analog Watch", WATCHES, 1995, 25, 4.1, 2600, 0,
                "A clean minimalist dial on a mesh strap", "Black", "Silver"));
        p.add(product("Garmin", "Forerunner 265 GPS Running Watch", WATCHES, 49990, 5, 4.8, 420, 10,
                "An AMOLED GPS running watch with training readiness and a race predictor", "Black", "White"));
        p.add(product("Garmin", "Instinct 2 Solar Smart Watch", WATCHES, 37990, 10, 4.6, 310, 8,
                "A rugged outdoor watch with solar charging", "Olive", "Black"));
        p.add(product("Noise", "ColorFit Pro 5 Smart Watch", WATCHES, 5999, 60, 4.0, 7200, 150,
                "A 1.85-inch AMOLED display with Bluetooth calling", "Black", "Grey"));

        // ---- Bags (10) ----
        p.add(product("American Tourister", "Casual Laptop Backpack 32L", BAGS, 3999, 55, 4.2, 5400, 120,
                "A padded 15.6-inch laptop sleeve and water-resistant fabric", "Black", "Navy", "Grey"));
        p.add(product("American Tourister", "Zing Sports Gym Bag", BAGS, 1799, 45, 4.1, 1600, 75,
                "A compact gym bag with a separate wet pocket", "Black", "Grey"));
        p.add(product("Skybags", "Campus Backpack 30L", BAGS, 2499, 60, 4.1, 6300, 140,
                "Three compartments and a bottle pocket for college days", "Blue", "Black", "Red"));
        p.add(product("Wildcraft", "Trailblazer 45L Rucksack", BAGS, 4999, 30, 4.4, 1100, 30,
                "An adjustable harness and a rain cover for weekend treks", "Olive", "Blue"));
        p.add(product("Nike", "Brasilia Training Duffel Bag", BAGS, 2995, 15, 4.4, 1400, 55,
                "A durable duffel with a ventilated shoe compartment", "Black", "Navy"));
        p.add(product("Adidas", "Linear Duffel Bag", BAGS, 2599, 30, 4.3, 1900, 60,
                "A spacious main compartment and a zip end pocket", "Black", "Blue"));
        p.add(product("Puma", "Phase Backpack", BAGS, 1999, 45, 4.2, 3100, 85,
                "A lightweight everyday backpack with a front zip pocket", "Black", "Red"));
        p.add(product("The North Face", "Borealis Backpack 28L", BAGS, 9999, 10, 4.7, 720, 20,
                "FlexVent suspension and a padded laptop sleeve", "Black", "Grey"));
        p.add(product("Fossil", "Buckner Leather Messenger Bag", BAGS, 14995, 30, 4.5, 260, 0,
                "A full-grain leather messenger with a laptop compartment", "Brown"));
        p.add(product("Decathlon", "Quechua NH100 Hiking Backpack 20L", BAGS, 499, 0, 4.3, 12000, 300,
                "A light daypack for short hikes", "Blue", "Black", "Olive"));

        // ---- Sports Accessories (12) ----
        p.add(product("Nike", "Dri-FIT Everyday Running Socks", SPORTS_ACCESSORIES, 1295, 10, 4.5, 2400, 160,
                "Cushioned Dri-FIT socks in a pack of three, with arch support", SOCK_SIZES, "White", "Black"));
        p.add(product("Adidas", "Cushioned Crew Running Socks", SPORTS_ACCESSORIES, 999, 20, 4.4, 1800, 150,
                "A pack of three crew socks with cushioned soles", SOCK_SIZES, "White", "Black"));
        p.add(product("Puma", "Sports Ankle Running Socks", SPORTS_ACCESSORIES, 699, 30, 4.2, 2900, 200,
                "A pack of three low-cut socks with a breathable mesh top", SOCK_SIZES, "Black", "Grey"));
        p.add(product("Decathlon", "Kalenji Running Belt", SPORTS_ACCESSORIES, 799, 0, 4.4, 3100, 120,
                "A slim waist belt that holds your phone and keys while you run", "Black"));
        p.add(product("Boldfit", "Yoga Mat 6mm", SPORTS_ACCESSORIES, 999, 60, 4.2, 9800, 250,
                "A non-slip, 6 mm thick TPE mat for yoga and floor workouts", "Blue", "Pink", "Black"));
        p.add(product("Boldfit", "Adjustable Skipping Rope", SPORTS_ACCESSORIES, 399, 50, 4.1, 7700, 300,
                "A tangle-free rope with ball bearings for cardio sessions", "Black"));
        p.add(product("Nike", "Dri-FIT Running Headband", SPORTS_ACCESSORIES, 895, 0, 4.3, 600, 90,
                "A Dri-FIT headband that keeps sweat out of your eyes", "Black", "White"));
        p.add(product("Under Armour", "Performance Running Cap", SPORTS_ACCESSORIES, 1799, 20, 4.4, 540, 70,
                "A lightweight cap with UPF 30+ sun protection", "Black", "White"));
        p.add(product("Decathlon", "Domyos Adjustable Dumbbell Set 20kg", SPORTS_ACCESSORIES, 3999, 0, 4.5, 4200, 40,
                "Cast-iron plates with threaded bars for home strength training", "Black"));
        p.add(product("Garmin", "HRM-Dual Heart Rate Chest Strap", SPORTS_ACCESSORIES, 7990, 10, 4.6, 380, 25,
                "Accurate heart-rate data over Bluetooth and ANT+", "Black"));
        p.add(product("Wildcraft", "Insulated Sports Water Bottle 750ml", SPORTS_ACCESSORIES, 899, 30, 4.3, 2200, 180,
                "Double-wall insulation keeps drinks cold for 24 hours", "Blue", "Black", "Silver"));
        p.add(product("Adidas", "Performance Training Gloves", SPORTS_ACCESSORIES, 1499, 25, 4.2, 400, 0,
                "Padded palms and a breathable back for gym sessions", "Black"));

        return List.copyOf(p);
    }

    /**
     * Demo engagement, spread over the last few days so that "trending" and co-interaction
     * recommendations have data on first start. Each shopper's session links related
     * products, for example Pegasus shoes, running socks and running shorts.
     */
    public static List<SeedInteraction> interactions() {
        List<SeedInteraction> i = new ArrayList<>();
        session(i, "aarav@quickfind.dev", 70,
                "Nike Pegasus 41 Running Shoes", "Nike Dri-FIT Everyday Running Socks",
                "Nike Challenger 7-inch Running Shorts", "Nike Dri-FIT Running T-Shirt");
        i.add(new SeedInteraction("aarav@quickfind.dev", "Nike Pegasus 41 Running Shoes", "ADD_TO_CART", null, 69));
        i.add(new SeedInteraction("aarav@quickfind.dev", "Nike Dri-FIT Running T-Shirt", "WISHLIST", null, 69));

        session(i, "diya@quickfind.dev", 50,
                "Adidas Ultraboost Light Running Shoes", "Adidas Cushioned Crew Running Socks",
                "Adidas Own The Run Running Shorts", "Adidas Own The Run T-Shirt");
        i.add(new SeedInteraction("diya@quickfind.dev", "Adidas Cushioned Crew Running Socks", "ADD_TO_CART", null, 49));
        i.add(new SeedInteraction("diya@quickfind.dev", "Adidas Own The Run T-Shirt", "WISHLIST", null, 49));

        session(i, "kabir@quickfind.dev", 30,
                "ASICS Gel-Kayano 31 Running Shoes", "Nike Dri-FIT Everyday Running Socks",
                "Garmin Forerunner 265 GPS Running Watch", "Decathlon Kalenji Running Belt");
        i.add(new SeedInteraction("kabir@quickfind.dev", "Garmin Forerunner 265 GPS Running Watch", "WISHLIST", null, 29));

        session(i, "meera@quickfind.dev", 20,
                "Adidas Samba OG Sneakers", "Levi's 511 Slim Fit Jeans",
                "Uniqlo AIRism Cotton Oversized T-Shirt", "Levi's Trucker Denim Jacket");
        i.add(new SeedInteraction("meera@quickfind.dev", "Levi's Trucker Denim Jacket", "WISHLIST", null, 19));

        session(i, "rohan@quickfind.dev", 12,
                "Casio G-Shock GA-2100 Digital Watch", "American Tourister Casual Laptop Backpack 32L",
                "Titan Neo Analog Watch", "Fossil Grant Chronograph Leather Watch");
        i.add(new SeedInteraction("rohan@quickfind.dev", "Casio G-Shock GA-2100 Digital Watch", "ADD_TO_CART", null, 11));

        session(i, "sara@quickfind.dev", 6,
                "Nike Pegasus 41 Running Shoes", "Nike Dri-FIT Challenger Running Track Pants",
                "Nike Dri-FIT Running Headband", "Nike Dri-FIT Everyday Running Socks");
        i.add(new SeedInteraction("sara@quickfind.dev", "Nike Dri-FIT Everyday Running Socks", "ADD_TO_CART", null, 5));

        session(i, DEMO_USER_EMAIL, 3,
                "Nike Pegasus 41 Running Shoes", "Adidas Ultraboost Light Running Shoes");
        i.add(new SeedInteraction(DEMO_USER_EMAIL, "Nike Dri-FIT Running T-Shirt", "WISHLIST", null, 2));
        i.add(new SeedInteraction(DEMO_USER_EMAIL, "Casio G-Shock GA-2100 Digital Watch", "WISHLIST", null, 2));

        String[][] searches = {
                {"aarav@quickfind.dev", "running shoes"}, {"diya@quickfind.dev", "running shoes"},
                {"kabir@quickfind.dev", "running shoes"}, {"sara@quickfind.dev", "running shoes"},
                {DEMO_USER_EMAIL, "running shoes"}, {"aarav@quickfind.dev", "black running shoes"},
                {"kabir@quickfind.dev", "black running shoes"}, {"sara@quickfind.dev", "black running shoes"},
                {"diya@quickfind.dev", "running socks"}, {"aarav@quickfind.dev", "running socks"},
                {"rohan@quickfind.dev", "smart watch"}, {"kabir@quickfind.dev", "smart watch"},
                {"rohan@quickfind.dev", "laptop backpack"}, {"meera@quickfind.dev", "laptop backpack"},
                {"meera@quickfind.dev", "denim jacket"}, {"sara@quickfind.dev", "gym t-shirt"}};
        int hoursAgo = 60;
        for (String[] search : searches) {
            i.add(new SeedInteraction(search[0], null, "SEARCH", search[1], hoursAgo));
            hoursAgo -= 3;
        }
        return List.copyOf(i);
    }

    private static void session(List<SeedInteraction> out, String email, int hoursAgo, String... productNames) {
        for (String name : productNames) {
            out.add(new SeedInteraction(email, name, "PRODUCT_VIEW", null, hoursAgo));
        }
    }

    private static SeedProduct product(String brand, String model, String subcategory, int price, int discount,
                                       double rating, int reviews, int stock, String highlight, String... colors) {
        return product(brand, model, subcategory, price, discount, rating, reviews, stock, highlight,
                defaultSizes(subcategory), colors);
    }

    private static SeedProduct product(String brand, String model, String subcategory, int price, int discount,
                                       double rating, int reviews, int stock, String highlight,
                                       List<String> sizes, String... colors) {
        String description = highlight + ". " + subcategoryDescription(subcategory);
        return new SeedProduct(brand, brand + " " + model, subcategory, price, discount, rating, reviews, stock,
                description, List.of(colors), sizes);
    }
}
