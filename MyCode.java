import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.io.FileReader;
import java.io.FileWriter;

import org.json.*;




public class MyCode {
	//Define a class called "item" to store the information of purchase time and amount for each transaction
	static class item {
		String id;				//user id
		String purchase_time;   //user's one purchase time
		Double purchase_amount; //user's one purchase amount
	}
	//Defina a class called "user_info" to store the inforamtion about one user, including id, list of friends, and list of his or her own purchase history 
	static class user_info {
		ArrayList <item> user_purchase_history;
		ArrayList <String> user_friends_id;
	}

	public static HashMap<String, user_info> user_List = new HashMap<String, user_info>(); // Store all the user information, on individual user base
	public static int Degree;		//Degree of social network, defined in the input file
	public static int Transaction;	//Number of transaction included in the mean and sd calculation
	public static ArrayList<String> already_checked_friends = new ArrayList<String>();	//List of users that have been already checked for their purchase transaction
	public static ArrayList<item> friends_purchases =new ArrayList<item>();				//List of one target user's friends' all purchases (within the degree of social network)

	public static void main(String[] args) {
		ArrayList<JSONObject> batch_File = readJSONFile("batch_log.json");	//store the information from batch_log.json
		ArrayList<JSONObject> stream_File = readJSONFile("stream_log.json");  //store the information from stram_log.json
		ArrayList<JSONObject> out_put = new ArrayList<JSONObject>();			 //store the output information; if none flagged transaction found, it will be null
		List<item> sorted_purchases = new ArrayList<item>();					 //a sub list of all the friends' transaction by the number of transaction and sorted by time
		ArrayList<item> flagged_purchases = new ArrayList<item>();			 //A list to store all the flagged purchase transactions if any. if there is none flagged transaction, it stays null

		Degree = Integer.parseInt((String) batch_File.get(0).get("D"));		//get the degree of social network from batch_log file
		Transaction = Integer.parseInt((String) batch_File.get(0).get("T"));  //get the number of transaction from batch_log file
		
		buildNetwork(batch_File);											//Collect the information from input files, organize it by each individual user and store properly
		buildNetwork(stream_File);

		Scanner input = new Scanner(System.in);								//Collect input of targeted user id
		System.out.println("Please enter the target user id");
		String initial_user = input.nextLine();

		while(!user_List.containsKey(initial_user)) {
			System.out.println("User does not exist, sorry");
			System.out.println("Please enter the target user id");
			initial_user=input.nextLine();
		}
		input.close();
		
		already_checked_friends.add(initial_user);							//add the targeted user into the list of already checked users to avoid including its own transactions
		go_through_friends(initial_user,0,Degree);							//search the targeted user's social network to find his or her friends within the given degree, as well as friends' purchase history


		Collections.sort(friends_purchases,new timeCompare());				//sort the list of friends purchase history by time, from most current to earlier ones

		double sum =0;
		double mean = 0;
		double standard_deviation = 0;
		double deviation =0;
		if(friends_purchases.size()<=2) {									//make sure only include the most recent transactions, by the given number of transaction
			out_put = null;
			flagged_purchases = null;
		}
		else {
			if(friends_purchases.size()<Transaction) {
				sorted_purchases = friends_purchases;
			}
			else {
				sorted_purchases = friends_purchases.subList(0, Transaction);      	
			}

			//In the final pool of friend's purchase, we need to calculate the mean and sd to flag the large purchase.

			mean = sum/sorted_purchases.size();
			for(int j = 0; j< sorted_purchases.size(); j++) {
				deviation = deviation+ ((sorted_purchases.get(j).purchase_amount-mean)*(sorted_purchases.get(j).purchase_amount-mean))/(sorted_purchases.size()-1);
			}
			standard_deviation = Math.sqrt(deviation);
		}
		//flag out those large transactions, which is above mean + 3*sd
		for(int j = 0; j< sorted_purchases.size(); j++) {
			if(sorted_purchases.get(j).purchase_amount>=mean+3*standard_deviation) {
				flagged_purchases.add(sorted_purchases.get(j));
			}
		}
		//transfer the flagged transaction information to the output JSONObject arraylist for writing into the output file
		for(int j = 0; j< flagged_purchases.size(); j++) {
			System.out.println(flagged_purchases.get(j).purchase_amount);
			out_put.get(j).put("event_type", "purchase");
			out_put.get(j).put("timestamp", flagged_purchases.get(j).purchase_time);
			out_put.get(j).put("id", flagged_purchases.get(j).id);
			out_put.get(j).put("amount", flagged_purchases.get(j).purchase_amount);
			out_put.get(j).put("mean", mean);
			out_put.get(j).put("sd", standard_deviation);
		}
		
		//export the output file
		try {
			FileWriter File = new FileWriter("flagged_purchases.json");
			if(out_put==null) {
		
				File.write("");
			}
			else {
				for(int m=0;m<out_put.size();m++) {
					System.out.println(m);
					File.write(out_put.get(m).toString());
					System.out.println(out_put.get(m).toString());
					System.getProperty("line.separator");
				}

			}
			File.close();
		}
		catch (IOException e){e.printStackTrace();}
		System.out.println("Finished!");

	}

	//Define a Comparator class to help sort purchase time (from most recent to earier ones)
	static class timeCompare implements Comparator<item>{
		public int compare(item i1, item i2) {
			return  -(int) (i1.purchase_time.compareTo(i2.purchase_time));
		}	
	}

	//read information from JSONFile, line by line,return an ArrayList of JSONObject of each event information
	private static ArrayList<JSONObject> readJSONFile(String FileName){
		ArrayList<JSONObject> Batch = new ArrayList<JSONObject>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(FileName));
			String Line  = br.readLine();
			while(Line!=null && Line.startsWith("{")) {
				JSONObject obj = new JSONObject(Line);
				Batch.add(obj);
				Line = br.readLine();
			}
			br.close();

		}
		catch(FileNotFoundException e) { e.printStackTrace();}
		catch(IOException e) { e.printStackTrace();}
		catch(Exception e) { e.printStackTrace();}
		return Batch;
	}

	//Read the information from JSONObject files and organize it by each individual user, including user id, user purchase history, and list of friends, store into a HashMap called user_List
	private static void buildNetwork(ArrayList<JSONObject> File) {
		ArrayList<JSONObject> input = File;
		JSONObject event = null;

		for (int i = 0; i < input.size();i++){
			event = input.get(i);
			try {
				if(event.get("D")!= null) {
					continue;
				};
			}
			catch(JSONException e) {}
			//if the event is purchase, use the add_user_purchase method to add into the user's purchase history
			if (event.get("event_type").equals("purchase")) {
				item new_purchase = new item();
				String id = new String();
				id = (String) event.get("id");
				new_purchase.id = (String) event.get("id");
				new_purchase.purchase_time = (String) event.get("timestamp");
				new_purchase.purchase_amount = Double.parseDouble((String) event.get("amount"));
				add_user_purchase(id,new_purchase);
			}
			//if the event is befriend, then use the add_user_friend method to make sure both of the users have the other user in their friends list
			if (event.get("event_type").equals("befriend")) {
				String id1 = null;
				String id2 = null;
				id1 = (String) event.get("id1");
				id2 = (String) event.get("id2");
				add_user_friend(id1, id2);
				add_user_friend(id2, id1);
			}
			//if the event is unfriend, then use the remove_user_friend method to remove the user from the other user's friends list, vice versa
			if (event.get("event_type").equals("unfriend")) {
				String id1 = null;
				String id2 = null;
				id1 = (String) event.get("id1");
				id2 = (String) event.get("id2");
				remove_user_friend(id1, id2);
				remove_user_friend(id2, id1);
			}
		}
	}

	//check if the user is already in the user list, if yes, add the purchase transaction into the user_info object and update the user List
	//if not in the user list, create a user_info object of the "new user" and added into the user List
	private static void add_user_purchase(String user_id, item details) {
		ArrayList<item> temp_List = new ArrayList<item>();
		if(user_List.containsKey(user_id)) {
			temp_List = user_List.get(user_id).user_purchase_history;
			temp_List.add(details);
			user_List.get(user_id).user_purchase_history = temp_List;
		}
		else {
			user_info new_user = new user_info();
			new_user.user_friends_id = new ArrayList<String>();
			new_user.user_purchase_history = new ArrayList<item>();
			user_List.put(user_id, new_user);
			user_List.get(user_id).user_purchase_history.add(details);

		}
	}

	//add friend's id into the user's list of friend; also add the user into the friend's list of friends
	private static void add_user_friend(String user_id, String friend_id) {
		ArrayList<String> temp_List = new ArrayList<String>();

		if(user_List.containsKey(user_id)) {
			temp_List = user_List.get(user_id).user_friends_id;
			temp_List.add(friend_id);
			user_List.get(user_id).user_friends_id = temp_List;
		}
		else {
			user_info new_user = new user_info();
			new_user.user_friends_id = new ArrayList<String>();
			new_user.user_purchase_history = new ArrayList<item>();
			user_List.put(user_id, new_user);
			user_List.get(user_id).user_friends_id.add(friend_id);
		}
	}

	//remove the friend's id from the user's list of friend; also remove this user's id from the friend's list of friends
	private static void remove_user_friend(String user_id, String friend_id) {
		ArrayList<String> current_friends = user_List.get(user_id).user_friends_id;
		if(!current_friends.isEmpty() && current_friends.contains(friend_id)){
			current_friends.remove(current_friends.indexOf(friend_id));
		}
		else {
			return;
		}

	}

	//Search through this user's social network within the given degree of friends to collect his or her friends' id and purchase history
	private static void go_through_friends(String user_id, int current_degree, int max_degree){
		ArrayList<String> friends_names = new ArrayList<String>();
		int current_depth;
		current_depth = current_degree;
		friends_names = user_List.get(user_id).user_friends_id;
		if(current_depth>=max_degree||friends_purchases.size()>=Transaction) {
			return;
		}
		else{for(String name:friends_names) {
			if(already_checked_friends.contains(name)) {
				continue;
			}
			else {
				already_checked_friends.add(name);
				friends_purchases.addAll(user_List.get(name).user_purchase_history);
			}
		}
		}
		current_depth=current_depth+1;
		for (String name:friends_names) {
			go_through_friends(name,current_depth, max_degree);
		}
	}
}




