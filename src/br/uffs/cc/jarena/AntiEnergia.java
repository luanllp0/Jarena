/**
 * Representa um ponto de antienergia no mapa. Os pontos de antienergia descarregam a
 * energia dos agentes que estiverem próximos a ele.
 *
 * 
 */

package br.uffs.cc.jarena;

public class AntiEnergia extends Entidade
{	
	private int velocidadeX;
	private int velocidadeY;
	private boolean andando;
	private long horaProximoEstado;
	private boolean movimentoAleatorio;
	private boolean indoParaDestino;
	private int destinoX;
	private int destinoY;
	
	public AntiEnergia(Integer x, Integer y, Integer energia) {
		super(x, y, energia);
		
		velocidadeX 		= (int)(Math.random() * Constants.PONTO_ANTI_ENERGIA_VELOCIDADE) * (Math.random() < 0.5 ? -1 : 1);
		velocidadeY 		= (int)(Math.random() * Constants.PONTO_ANTI_ENERGIA_VELOCIDADE) * (Math.random() < 0.5 ? -1 : 1);
		andando 			= false;
		horaProximoEstado 	= 0;
		movimentoAleatorio	= true;
		// começa com movimento aleatório
	}
	
	public void update() {
		if(isMorta()) {
			return;
		}
		
		ganhaEnergia(Constants.PONTO_ANTI_ENERGIA_REGENERA_TURNO);
		
		for(Entidade a : getArena().getEntidades()) {
			if((a instanceof Agente) && (distancia(a) <= Constants.PONTO_ANTI_ENERGIA_AREA)) {
				descarregaEnergiaAgente((Agente) a);
			}
		}

		if (indoParaDestino) {
			movimentaAteDestino();
		} else if (movimentoAleatorio && Constants.PONTO_ANTI_ENERGIA_MOVEL) {
			movimenta();
		}
	}
	
	private void movimenta() {
		long tempoAgora 				= System.currentTimeMillis();
		long tempoRestanteMudarEstado 	= horaProximoEstado - tempoAgora;
		
		if(andando) {
			evitaSairTela();
			alteraX(velocidadeX);
			alteraY(velocidadeY);
		}
		
		if(tempoRestanteMudarEstado <= 0) {
			andando = !andando;
			horaProximoEstado = tempoAgora + (andando ? Constants.PONTO_ANTI_ENERGIA_TEMPO_ANDANDO : Constants.PONTO_ANTI_ENERGIA_TEMPO_DESCANSO);
		}
	}

	private void movimentaAteDestino() {
		int distanciaX = destinoX - getX();
		int distanciaY = destinoY - getY();
		int passoX = Math.min(Math.abs(distanciaX), Constants.PONTO_ANTI_ENERGIA_VELOCIDADE_DESTINO);
		int passoY = Math.min(Math.abs(distanciaY), Constants.PONTO_ANTI_ENERGIA_VELOCIDADE_DESTINO);

		if (distanciaX < 0) {
			passoX = -passoX;
		}
		if (distanciaY < 0) {
			passoY = -passoY;
		}

		alteraX(passoX);
		alteraY(passoY);

		if (getX() == destinoX && getY() == destinoY) {
			indoParaDestino = false;
		}
	}
	// move ponto até o destino
	
	private void evitaSairTela() {
		boolean movendoDireita 	= velocidadeX > 0;
		boolean movendoCima 	= velocidadeY < 0;
		
		if(movendoDireita && getX() >= Constants.LARGURA_MAPA) {
			velocidadeX *= -1;
		}

		if(!movendoDireita && getX() <= 0) {
			velocidadeX *= -1;
		}

		if(movendoCima && getY() <= 0) {
			velocidadeY *= -1;
		}

		if(!movendoCima && getY() >= Constants.ALTURA_MAPA) {
			velocidadeY *= -1;
		}
	}
	
	private void descarregaEnergiaAgente(Agente a) {
		// Primeiro, descontamos a energia do agente.
		a.gastaEnergia(Constants.PONTO_ANTI_ENERGIA_ENTREGA_TURNO);
		ganhaEnergia(Constants.PONTO_ANTI_ENERGIA_ENTREGA_TURNO);		
		
		// Depois avisamos o agente que ele perdeu energia.
		a.sinalizaPerdeuEnergia();

		System.out.println("AntiEnergia"+getId()+" tirando vida de " + a);
	}

	public void defineDestino(int x, int y) {
		destinoX = x;
		destinoY = y;
		movimentoAleatorio = false;
		indoParaDestino = true;
	}
	// desativa movimento aleatorio e define qual o destino do ponto
	
	public String toString() {
		return "[AntiEnergia" + getId()+"] energia="+getEnergia()+", x="+getX()+", y="+getY();
	}
}
