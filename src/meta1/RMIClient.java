package meta1;

import java.util.Scanner;

public class RMIClient {

    private void printmenu() {
        System.out.println("--------Menu--------");
        System.out.println("1) Index URL");
        System.out.println("2) Search for terms");
        System.out.println("3) List pages linked to a specific page");
        System.out.println("4) Administration page");
        System.out.println("5) Exit");
        System.out.println("--------------------")
        System.out.println("Choose an option:")

    }

    private static isInt(Scanner sc){
        while(!sc.hasNextInt()){
            System.out.println("It must be an integer!");
            sc.next();
        }
        int opt = sc.nextInt();
        return opt;
    }

    public void options(int option) {

    }

    public static void main(String[] args) {
        String 
    }
}
