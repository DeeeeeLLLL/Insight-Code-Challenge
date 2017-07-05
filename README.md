# Insight-Code-Challenge

My name is Di Liang, I used Java to implement this challenge.An external package (JSON package) from Maven was used to help read in JSONObject type from the input files.

####Defined objects#####
Two object types are defined in this project: 1. item: for each purchase transation, including id (String), purchase_time (String), and purchase_amount(double); 2. user_info: for each user, including id (String), user_friends_list (ArrayList<String>), and user_purchase_history(ArrayList<item>).

####Main method#####
The main method calls several private methods to read in the JSONObject file and orgainize the information to individual user object. All the user object (user_info) was stored in a HashMap called "user_List".In the main method, the program prompts to input of initial targeted use id. After that, the program searches for the targeted user's friends within the given degree of social network and stores the purchase history of all selected friends into an ArrayList. In the end, the list of friends' purchase transactions are sorted from most recent to earlier ones. The first certain number (the given number of transaction) of transactions (POOL) were selected to calculate the mean and standard deviation. Afterwards, any purchase transaction from the POOL that are greater than mean + 3* standard deviation is the flagged purchases and exported into the output file.

####Private methods#####
  readJSONFiles: read information from JSONFile, line by line,return an ArrayList of JSONObject of each event information
	buildNetwork:  read the information from JSONObject files and organize it by each individual user, including user id, user    purchase history, and list of friends, store into a HashMap called user_List
  add_user_purchase: check if the user is already in the user list, if yes, add the purchase transaction into the user_info object and update the user List. if not in the user list, create a user_info object of the "new user" and added into the user List
  add_user_friend: add friend's id into the user's list of friend; also add the user into the friend's list of friends  
  remove_user_friend: remove the friend's id from the user's list of friend; also remove this user's id from the friend's list of friends
  go_through_friends: Search through this user's social network within the given degree of friends to collect his or her friends' id and purchase history
