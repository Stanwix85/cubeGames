package cubeGame.demo;

import fr.le_campus_numerique.square_games.engine.connectfour.ConnectFourGameFactory;
import fr.le_campus_numerique.square_games.engine.taquin.TaquinGameFactory;
import fr.le_campus_numerique.square_games.engine.tictactoe.TicTacToeGameFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	@Bean
	public TicTacToeGameFactory ticTacToeGameFactory() {
		return new TicTacToeGameFactory();
	}

	@Bean
	public TaquinGameFactory taquinGameFactory() {
		return new TaquinGameFactory();
	}

	@Bean
	public ConnectFourGameFactory connectFourGameFactory() {
		return new ConnectFourGameFactory();
	}

}
