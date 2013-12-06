import java.util.Scanner;


public class Model {
	private int numGuess; 

	public int getNumGuess() {
		return numGuess;
	}



	public void setNumGuess(int numGuess) {
		this.numGuess = numGuess;
	}



	public void message(String msg) {

		System.out.println(msg);
	}


	public int guess(int userGuess) {

		if (getNumGuess() > userGuess) {
			System.out.println("Guessed Too Low!");
			return 1;
		}else if (getNumGuess() < userGuess) {
			System.out.println("Guessed Too High!");
			return -1;
		} 
		else{
			System.out.println("Guess is Correct!");
			return 0; 
		}
	}



}