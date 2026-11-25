import java.io.*;
import java.util.*;

public class Main {

    // Ticket class representing a confirmed booking
    static class Ticket {
        String id;
        String name;
        String seat;

        Ticket(String id, String name, String seat) {
            this.id = id;
            this.name = name;
            this.seat = seat;
        }
    }

    // WaitingNode class representing a customer in the PriorityQueue
    static class WaitingNode implements Comparable<WaitingNode> {
        String name;
        String membership; // "Gold" or "Regular"
        long arrivalOrder;

        WaitingNode(String name, String membership, long arrivalOrder) {
            this.name = name;
            this.membership = membership;
            this.arrivalOrder = arrivalOrder;
        }

        private int getPriorityValue() {
            return membership.equalsIgnoreCase("Gold") ? 2 : 1;
        }

        @Override
        public int compareTo(WaitingNode other) {
            if (this.getPriorityValue() != other.getPriorityValue()) {
                return Integer.compare(other.getPriorityValue(), this.getPriorityValue());
            }
            return Long.compare(this.arrivalOrder, other.arrivalOrder);
        }
    }

    // Data Structures as specified in PS 15
    private static final int ROWS = 5; // Rows A-E
    private static final int COLS = 5; // Seats 1-5
    private static String[][] seats = new String[ROWS][COLS]; // 2D Array (null = free, bookingId = occupied)
    private static HashMap<String, Ticket> bookingMap = new HashMap<>(); // Hashing for ID lookup
    private static PriorityQueue<WaitingNode> waitingList = new PriorityQueue<>(); // PriorityQueue (Gold before Regular)

    private static int nextBookingNumber = 101; // First booking ID is T101
    private static long arrivalCounter = 0;
    private static final String FILE_NAME = "tickets.txt";

    public static void main(String[] args) {
        loadFromFile();
        Scanner sc = new Scanner(System.in);

        while (true) {
            System.out.println("\n=== MOVIE TICKET BOOKING SYSTEM ===");
            System.out.println("1. View Seats");
            System.out.println("2. Book Seat");
            System.out.println("3. Cancel Booking");
            System.out.println("4. View Waiting List");
            System.out.println("5. View All Bookings (Sorted by ID)");
            System.out.println("6. Exit & Save");
            System.out.print("Enter choice: ");

            int choice;
            try {
                choice = Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Enter a number between 1 and 6.");
                continue;
            }

            switch (choice) {
                case 1:
                    viewSeats();
                    break;
                case 2:
                    bookSeat(sc);
                    break;
                case 3:
                    cancelBooking(sc);
                    break;
                case 4:
                    viewWaitingList();
                    break;
                case 5:
                    viewSortedBookings();
                    break;
                case 6:
                    saveToFile();
                    System.out.println("Data saved to " + FILE_NAME + ". Exiting...");
                    sc.close();
                    return;
                default:
                    System.out.println("Invalid option. Choose 1-6.");
            }
        }
    }

    // 1. View 5x5 2D Array Seat Layout
    public static void viewSeats() {
        System.out.println("\n--- SEAT LAYOUT (O = Available, X = Booked) ---");
        System.out.println("    1   2   3   4   5");
        for (int r = 0; r < ROWS; r++) {
            char rowLabel = (char) ('A' + r);
            System.out.print(rowLabel + "  ");
            for (int c = 0; c < COLS; c++) {
                System.out.print(seats[r][c] == null ? "[O] " : "[X] ");
            }
            System.out.println();
        }
    }

    // 2. Book Seat or Add to PriorityQueue Waiting List if Hall is Full
    public static void bookSeat(Scanner sc) {
        System.out.print("Name: ");
        String name = sc.nextLine().trim();

        if (isHallFull()) {
            System.out.print("Membership (Gold/Regular): ");
            String membership = sc.nextLine().trim();
            if (!membership.equalsIgnoreCase("Gold")) {
                membership = "Regular";
            } else {
                membership = "Gold";
            }
            waitingList.offer(new WaitingNode(name, membership, ++arrivalCounter));
            System.out.println("Hall full. " + name + " added to waiting list (" + membership + ").");
            return;
        }

        System.out.print("Seat: ");
        String seatInput = sc.nextLine().trim().toUpperCase();

        if (!isValidSeat(seatInput)) {
            System.out.println("Invalid seat. Choose rows A-E and seats 1-5.");
            return;
        }

        int row = seatInput.charAt(0) - 'A';
        int col = Integer.parseInt(seatInput.substring(1)) - 1;

        if (seats[row][col] != null) {
            System.out.println("Seat " + seatInput + " is already booked.");
            return;
        }

        String bookingId = "T" + (nextBookingNumber++);
        seats[row][col] = bookingId;
        Ticket ticket = new Ticket(bookingId, name, seatInput);
        bookingMap.put(bookingId, ticket);

        System.out.println("Booking confirmed. ID: " + bookingId + " | " + name + " | Seat " + seatInput);
        if (isHallFull()) {
            System.out.println("Hall is now full.");
        }
    }

    // 3. Cancel Booking via HashMap & Auto-Assign to Highest Priority in Queue
    public static void cancelBooking(Scanner sc) {
        System.out.print("Enter Booking ID to cancel: ");
        String bookingId = sc.nextLine().trim().toUpperCase();

        Ticket removedTicket = bookingMap.remove(bookingId);
        if (removedTicket == null) {
            System.out.println("Booking ID " + bookingId + " not found.");
            return;
        }

        String freedSeat = removedTicket.seat;
        int row = freedSeat.charAt(0) - 'A';
        int col = Integer.parseInt(freedSeat.substring(1)) - 1;
        seats[row][col] = null;

        if (!waitingList.isEmpty()) {
            WaitingNode nextPerson = waitingList.poll();
            String newBookingId = "T" + (nextBookingNumber++);
            seats[row][col] = newBookingId;
            bookingMap.put(newBookingId, new Ticket(newBookingId, nextPerson.name, freedSeat));

            System.out.println("Booking " + bookingId + " cancelled. Seat " + freedSeat +
                    " given to " + nextPerson.name + " (" + nextPerson.membership + ").");
        } else {
            System.out.println("Booking " + bookingId + " cancelled. Seat " + freedSeat + " is now available.");
        }
    }

    // 4. View PriorityQueue Waiting List (Sorted by Priority)
    public static void viewWaitingList() {
        if (waitingList.isEmpty()) {
            System.out.println("Waiting list is empty.");
            return;
        }
        System.out.println("\n--- WAITING LIST (Priority Order) ---");
        List<WaitingNode> tempList = new ArrayList<>(waitingList);
        Collections.sort(tempList); // Sorting by priority and arrival order
        int pos = 1;
        for (WaitingNode node : tempList) {
            System.out.println(pos++ + ". " + node.name + " (" + node.membership + ")");
        }
    }

    // 5. Sorting Confirmed Bookings by Booking ID
    public static void viewSortedBookings() {
        if (bookingMap.isEmpty()) {
            System.out.println("No confirmed bookings.");
            return;
        }
        List<Ticket> list = new ArrayList<>(bookingMap.values());
        list.sort(Comparator.comparing(t -> t.id));
        System.out.println("\n--- CONFIRMED BOOKINGS (Sorted by ID) ---");
        for (Ticket t : list) {
            System.out.println("ID: " + t.id + " | " + t.name + " | Seat " + t.seat);
        }
    }

    // Helper: Validate Seat Format (A1 to E5)
    private static boolean isValidSeat(String seat) {
        if (seat == null || seat.length() != 2) return false;
        char r = seat.charAt(0);
        char c = seat.charAt(1);
        return (r >= 'A' && r <= 'E') && (c >= '1' && c <= '5');
    }

    // Helper: Check if all 25 seats in 2D array are booked
    private static boolean isHallFull() {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (seats[r][c] == null) return false;
            }
        }
        return true;
    }

    // File Handling: Load bookings from tickets.txt at start
    private static void loadFromFile() {
        File file = new File(FILE_NAME);
        if (!file.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 3) {
                    String id = parts[0].trim();
                    String name = parts[1].trim();
                    String seat = parts[2].trim();
                    if (isValidSeat(seat)) {
                        int r = seat.charAt(0) - 'A';
                        int c = Integer.parseInt(seat.substring(1)) - 1;
                        seats[r][c] = id;
                        bookingMap.put(id, new Ticket(id, name, seat));

                        int num = Integer.parseInt(id.substring(1));
                        if (num >= nextBookingNumber) {
                            nextBookingNumber = num + 1;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error loading previous bookings: " + e.getMessage());
        }
    }

    // File Handling: Save bookings to tickets.txt on exit
    private static void saveToFile() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(FILE_NAME))) {
            for (Ticket t : bookingMap.values()) {
                pw.println(t.id + "," + t.name + "," + t.seat);
            }
        } catch (IOException e) {
            System.out.println("Error saving bookings: " + e.getMessage());
        }
    }
}