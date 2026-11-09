/**
 * Descreve uma arena para combate entre agentes. A arena contém uma lista com agentes
 * vivos e uma lista com os pontos de energia. Em cada iteração, todos os agentes vivos
 * são processados.
 * 
 * Fernando Bevilacqua <fernando.bevilacqua@uffs.edu.br>
 */

package br.uffs.cc.jarena;

import java.awt.event.KeyEvent;
import java.util.Vector;
import java.lang.reflect.Constructor;

import br.uffs.cc.jarena.renders.simple2d.*;

public class Arena implements Runnable {
	private Vector<Entidade> entidades;
	private Vector<Entidade> nascendo;
	private Vector<Entidade> morrendo;
	private Desenhista desenhista;
	private Estatistico estatistico;
	private Teclado teclado;
	private long ultimoUpdate;
	private long horaInicio;
	private boolean ativa;
	private long intervaloUpdate;
	private boolean debug;

	public Arena() {
		// Inicializamos as coisas da arena (agentes, energia, etc)
		criaAmbiente();
		adicionaPontosEnergia();
		adicionaPontosAntiEnergia();
		adicionaAgentes();

		// Depois que tudo estiver configurado, arrumamos a tela
		// e iniciamos a aplicação
		initTela();
	}

	private void initTela() {
		this.desenhista = new DesenhistaSimples2D(); // TODO: usar o render definido no config.
		this.desenhista.init(this, teclado);
	}

	private void criaAmbiente() {
		ativa			= true;
		ultimoUpdate 	= 0;
		intervaloUpdate	= Constants.INTERVALO_UPDATE;
		entidades 		= new Vector<Entidade>();
		nascendo 		= new Vector<Entidade>();
		morrendo 		= new Vector<Entidade>();
		estatistico 	= new Estatistico(this);
		teclado			= new Teclado();
	}

	private void adicionaAgentes() {
		int i;

		for (i = 0; i < 15; i++) {
			adicionaEntidade(new AgenteDummy(0, 0, Constants.ENTIDADE_ENERGIA_INICIAL));						
			adicionaEntidade(new AgenteInimigo((int)(Constants.LARGURA_TELA * 0.95), 0, Constants.ENTIDADE_ENERGIA_INICIAL));						
		}
	}

	private void adicionaPontosEnergia() {
		double rand;
		int i, j;
		
		j = Constants.ALTURA_TELA/Constants.PONTO_ENERGIA_QUANTIDADE;

		for (i = 0; i < Constants.PONTO_ENERGIA_QUANTIDADE; i++) {
			rand = Math.random();
			adicionaEntidade(new PontoEnergia((int) (Constants.LARGURA_TELA * 0.95 * rand), j * i, Constants.PONTO_ENERGIA_SUPRIMENTO_INICIAL));
		}
	}

	private void adicionaPontosAntiEnergia() {
		int maxX = Constants.LARGURA_MAPA;
		int maxY = Constants.ALTURA_MAPA;
		int faixaX = maxX / 4;
		int faixaY = maxY / 4;
		int centroMinX = faixaX;
		int centroMaxX = maxX - faixaX;
		int centroMinY = faixaY;
		int centroMaxY = maxY - faixaY;
		// definição dos limites do mapa, as faixas dos cantos e a região central

		int[][] cantos = {
			{0, 0}, {maxX, 0}, {0, maxY}, {maxX, maxY}
		};

		for (int i = 0; i < Constants.PONTO_ANTI_ENERGIA_QUANTIDADE; i++) {
			int x;
			int y;

			if (i < Constants.PONTO_ANTI_ENERGIA_FIXOS) {
				switch (i) {
					case 0:
						x = coordenadaAleatoria(0, faixaX);
						y = coordenadaAleatoria(0, faixaY);
						break;
					case 1:
						x = coordenadaAleatoria(centroMaxX, maxX);
						y = coordenadaAleatoria(0, faixaY);
						break;
					case 2:
						x = coordenadaAleatoria(0, faixaX);
						y = coordenadaAleatoria(centroMaxY, maxY);
						break;
					default:
						x = coordenadaAleatoria(centroMaxX, maxX);
						y = coordenadaAleatoria(centroMaxY, maxY);
						break;
				}
				// associa os quatro primeiros pontos a um canto e sorteia sua posição inicial na região correspondente
			} else {
				x = coordenadaAleatoria(centroMinX, centroMaxX);
				y = coordenadaAleatoria(centroMinY, centroMaxY);
			}
			// sorteia a posição dos dois pontos móveis dentro da região central.

			AntiEnergia ponto = new AntiEnergia(x, y, Constants.PONTO_ANTI_ENERGIA_SUPRIMENTO_INICIAL);

			if (i < Constants.PONTO_ANTI_ENERGIA_FIXOS) {
				ponto.defineDestino(cantos[i][0], cantos[i][1]);
			}
			// define o destino de canto para os quatro pontos fixos.

			adicionaEntidade(ponto);
		}
	}

	private int coordenadaAleatoria(int minimo, int maximo) {
		return minimo + (int) (Math.random() * (maximo - minimo + 1));
	}
	// método auxiliar que sorteia uma coordenada entre dois limites.

	public Vector<Entidade> getEntidades() {
		return this.entidades;
	}
	
	public Desenhista getDesenhista() {
		return this.desenhista;
	}

	public void agendaNascimento(Entidade e) {
		this.nascendo.add(e);
	}

	public void agendaMorte(Entidade e) {
		System.out.println("[MORTE] " + e);
		this.morrendo.add(e);
	}

	public void run() {
		long agora;
		
		horaInicio = System.currentTimeMillis();
		
		while (ativa) {
			agora = System.currentTimeMillis();
			
			if ((agora - ultimoUpdate) >= intervaloUpdate) {
				update();
				ultimoUpdate = agora;
			}
			
			processaTeclado();
			estatistico.colheEstatisticas();
			desenhista.render();
		}
		
		estatistico.imprimeEstatisticas();
		desenhista.terminate();
	}
	
	public void termina() {
		ativa = false;
	}
	
	private void processaTeclado() {
		if(teclado.isKeyDown(KeyEvent.VK_UP)) {
			intervaloUpdate -= Constants.INTERVALO_UPDATE_INCREMENTO;
			
		} else if(teclado.isKeyDown(KeyEvent.VK_DOWN)) {
			intervaloUpdate += Constants.INTERVALO_UPDATE_INCREMENTO;
			
		} else if(teclado.isKeyDown(KeyEvent.VK_RIGHT) || teclado.isKeyDown(KeyEvent.VK_LEFT)) {
			intervaloUpdate = Constants.INTERVALO_UPDATE;
		}
		
		if(teclado.isKeyDown(KeyEvent.VK_Q) || teclado.isKeyDown(KeyEvent.VK_ESCAPE)) {
			termina();
		}
		
		if(teclado.isKeyDown(KeyEvent.VK_D)) {
			debug = true;
		} else {
			debug = false;
		}
	}

	private void update() {
		for (Entidade e : entidades) {
			if (!e.isMorta()) {
				try {
					e.update();
				} catch (Exception exp) {
					System.out.println("*** EXCECAO em entidade *** Quem = " + e);
					exp.printStackTrace();
				}
			}
		}

		if (nascendo.size() > 0) {
			for (Entidade j : nascendo) {
				adicionaEntidade(j);
			}
			nascendo.removeAllElements();
		}

		if (morrendo.size() > 0) {
			entidades.removeAll(morrendo);
			morrendo.removeAllElements();
		}
		
		if(isFimCombate()) {
			termina();
		}
	}
	
	private boolean isFimCombate() {
		boolean temAlguemNascendo 	= nascendo.size() > 0;
		boolean temAgentes 			= false;
		
		for(Entidade a : entidades) {
			if(a instanceof Agente && !a.isMorta()) {
				temAgentes = true;
				break;
			}
		}
		
		return !temAgentes && !temAlguemNascendo;
	}

	public void adicionaEntidade(Entidade e) {
		e.setArena(this);
		entidades.add(e);

		System.out.println("[NASCEU] " + e);
	}

	public void removeEntidade(Entidade entidade) {
		agendaMorte(entidade);
	}

	public void divideEntidade(Entidade entidade) {
		try {
			Class[] argsConstrutor = new Class[] { Integer.class, Integer.class, Integer.class };

			Class<? extends Entidade> classe = entidade.getClass();
			Constructor<? extends Entidade> construtor = classe.getConstructor(argsConstrutor);

			Entidade nova = construtor.newInstance(entidade.getX(), entidade.getY(), entidade.getEnergia());
			agendaNascimento(nova);
			
			if(entidade instanceof Agente) {
				estatistico.contabilizaDivisao((Agente)entidade);
				desenhista.agenteClonou((Agente)entidade, (Agente)nova);
			}
		} catch (Exception e) {
			System.out.println("Erro na hora de dividir a entidade!" + e.getMessage());
		}
	}
	
	public long getTimestampInicio() {
		return horaInicio;
	}
	
	public boolean isDebug() {
		return debug;
	}
}