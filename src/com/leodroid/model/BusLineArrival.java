package com.leodroid.model;

public class BusLineArrival {
	public String name;
	public String number;
	public String vehicle;
	public int arrival;

	public BusLineArrival(String arrivalResponse) {
		String arr[] = arrivalResponse.split(",");

		switch (arr.length) {
		case 4:
			this.vehicle = arr[0];
			this.number = arr[1];
			this.name = arr[2];
			try {
				this.arrival = Integer.parseInt(arr[3]);
			} catch (NumberFormatException e) {
			}
			break;
		case 3:

			this.number = arr[0];
			this.name = arr[1];
			try {
				this.arrival = Integer.parseInt(arr[2]);
			} catch (NumberFormatException e) {
			}
			break;
		case 2:
			this.vehicle = arr[0];

			try {
				this.arrival = Integer.parseInt(arr[1]);
			} catch (NumberFormatException e) {
			}
			break;
		default:
			break;
		}

	}
}
