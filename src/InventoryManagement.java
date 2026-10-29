
import java.sql.*;
import java.util.Scanner;

public class InventoryManagement {

    static final String URL = "jdbc:mysql://localhost:3306/retail_db";
    static final String USER = "root";
    static final String PASSWORD = "84459";

    static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {

        try (Connection con =
                     DriverManager.getConnection(URL, USER, PASSWORD)) {

            System.out.println("Connected to MySQL successfully!");

            int choice;

            do {
                System.out.println("\n===== INVENTORY MANAGEMENT SYSTEM =====");
                System.out.println("1. Insert Product Records");
                System.out.println("2. Retrieve Products by Category");
                System.out.println("3. Update Product Stock");
                System.out.println("4. Search and Modify Product Price");
                System.out.println("5. Inventory Value and Conditional Deletion");
                System.out.println("0. Exit");
                System.out.print("Enter your choice: ");

                choice = sc.nextInt();
                sc.nextLine();

                switch (choice) {
                    case 1:
                        insertProducts(con);
                        break;

                    case 2:
                        retrieveProducts(con);
                        break;

                    case 3:
                        updateStock(con);
                        break;

                    case 4:
                        updatePrice(con);
                        break;

                    case 5:
                        inventoryValueAndDelete(con);
                        break;

                    case 0:
                        System.out.println("Program terminated.");
                        break;

                    default:
                        System.out.println("Invalid choice!");
                }

            } while (choice != 0);

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    // (a) INSERT AT LEAST THREE PRODUCTS
    static void insertProducts(Connection con) throws SQLException {

        String sql = "INSERT INTO Product VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(sql)) {

            for (int i = 1; i <= 3; i++) {

                System.out.println("\nEnter details of product " + i);

                System.out.print("Product ID: ");
                int id = sc.nextInt();
                sc.nextLine();

                System.out.print("Product Name: ");
                String name = sc.nextLine();

                System.out.print("Category: ");
                String category = sc.nextLine();

                System.out.print("Quantity: ");
                int quantity = sc.nextInt();

                System.out.print("Price: ");
                double price = sc.nextDouble();
                sc.nextLine();

                if (quantity < 0 || price < 0) {
                    System.out.println(
                            "Quantity and price cannot be negative.");
                    i--;
                    continue;
                }

                ps.setInt(1, id);
                ps.setString(2, name);
                ps.setString(3, category);
                ps.setInt(4, quantity);
                ps.setDouble(5, price);

                try {
                    ps.executeUpdate();
                    System.out.println("Product inserted successfully!");

                } catch (SQLIntegrityConstraintViolationException e) {
                    System.out.println(
                            "Duplicate product ID! Record not inserted.");
                    i--;
                }
            }
        }
    }

    // (b) CONDITIONAL RETRIEVAL AND SORTING
    static void retrieveProducts(Connection con) throws SQLException {

        System.out.print("Enter category: ");
        String category = sc.nextLine();

        String sql = "SELECT * FROM Product " +
                "WHERE category = ? AND quantity < 20 " +
                "ORDER BY price DESC";

        try (PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, category);

            try (ResultSet rs = ps.executeQuery()) {

                boolean found = false;

                while (rs.next()) {
                    found = true;
                    displayProduct(rs);
                }

                if (!found) {
                    System.out.println("No matching products found.");
                }
            }
        }
    }

    // (c) UPDATE PRODUCT STOCK
    static void updateStock(Connection con) throws SQLException {

        System.out.print("Enter product ID: ");
        int id = sc.nextInt();

        System.out.print("Quantity to add: ");
        int quantity = sc.nextInt();
        sc.nextLine();

        if (quantity <= 0) {
            System.out.println("Quantity to add must be positive.");
            return;
        }

        String sql = "UPDATE Product " +
                "SET quantity = quantity + ? " +
                "WHERE product_id = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, quantity);
            ps.setInt(2, id);

            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println("Stock updated successfully!");
            } else {
                System.out.println("Product does not exist.");
            }
        }
    }

    // (d) SEARCH AND MODIFY PRODUCT PRICE
    static void updatePrice(Connection con) throws SQLException {

        System.out.print("Enter product ID: ");
        int id = sc.nextInt();

        System.out.print("Enter new price: ");
        double newPrice = sc.nextDouble();
        sc.nextLine();

        if (newPrice <= 0) {
            System.out.println("Price must be greater than zero.");
            return;
        }

        String selectSql =
                "SELECT * FROM Product WHERE product_id = ?";

        try (PreparedStatement ps = con.prepareStatement(selectSql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {

                if (!rs.next()) {
                    System.out.println("Product does not exist.");
                    return;
                }

                System.out.println("\nExisting product details:");
                displayProduct(rs);
            }
        }

        String updateSql =
                "UPDATE Product SET price = ? WHERE product_id = ?";

        try (PreparedStatement ps = con.prepareStatement(updateSql)) {

            ps.setDouble(1, newPrice);
            ps.setInt(2, id);

            ps.executeUpdate();
        }

        System.out.println("Price updated successfully!");

        try (PreparedStatement ps = con.prepareStatement(selectSql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    System.out.println("\nUpdated product details:");
                    displayProduct(rs);
                }
            }
        }
    }

    // (e) AGGREGATE QUERY AND CONDITIONAL DELETION
    static void inventoryValueAndDelete(Connection con)
            throws SQLException {

        String sumSql =
                "SELECT COALESCE(SUM(quantity * price), 0) " +
                        "AS total_value FROM Product";

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sumSql)) {

            if (rs.next()) {
                System.out.printf("Total inventory value: %.2f%n",
                        rs.getDouble("total_value"));
            }
        }

        System.out.print("Enter product ID to delete: ");
        int id = sc.nextInt();
        sc.nextLine();

        String deleteSql =
                "DELETE FROM Product " +
                        "WHERE product_id = ? AND quantity = 0";

        try (PreparedStatement ps = con.prepareStatement(deleteSql)) {

            ps.setInt(1, id);

            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println("Product deleted successfully!");
            } else {
                System.out.println(
                        "Cannot delete: product does not exist " +
                                "or its quantity is not zero.");
            }
        }
    }

    // COMMON METHOD TO DISPLAY PRODUCT DETAILS
    static void displayProduct(ResultSet rs) throws SQLException {

        System.out.println(
                "ID: " + rs.getInt("product_id") +
                        " | Name: " + rs.getString("product_name") +
                        " | Category: " + rs.getString("category") +
                        " | Quantity: " + rs.getInt("quantity") +
                        " | Price: " + rs.getDouble("price"));
    }
}
