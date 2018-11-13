package bots;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;


import arena.BattleBotArena;
import arena.BotInfo;
import arena.Bullet;

import java.util.Arrays;
import java.util.Random;

/**
 * MegaBot is a bot that collects information of nearby bots 
 * and makes the best moves accordingly. It can dodge bullets, 
 * avoid collisions with alive/dead bots or edges, attack other
 * bots, and follow them as well. MegaBot is not interested in trash 
 * talking or changing its icon too often.
 * 
 * The strategy is to defend any threats and when there are very few, attack.
 * For a full description of strategy and implementation, please visit:
 * ----------------------------------------------------------------------
 * https://drive.google.com/file/d/1ZP9UMjTrq0ztAZ9VXOIu0zZ18kyvrz8s/view?usp=sharing
 *-----------------------------------------------------------------------
 *
 * @author Sufiyaan Nadeem
 * @version 1.4 (November 11, 2018)
 */

public class MegaBot extends Bot {
	/**
	 * Next message to send, or null if nothing to send.
	 */
	private String nextMessage = null;
	/**
	 * An array of trash talk messages.
	 */
	private String[] killMessages = {"Mega Bot:::::Destroy"};
	/**
	 * Bot image
	 */
	Image current, up, down, right, left;
	/**
	 * My name (set when getName() first called)
	 */
	private String name = null;
	/**
	 * Counter for timing moves in different directions
	 */
	private int moveCount = 99;
	/**
	 * Next move to make
	 */
	private int move = BattleBotArena.UP;
	/**
	 * Counter to pause before sending a victory message
	 */
	private int msgCounter = 0;
	/**
	 * Used to decide if this bot should overheat or not
	 */
	private int targetNum = (int)(Math.random()*BattleBotArena.NUM_BOTS);
	/**
	 * The amount to sleep to simulate overheating because of excessive CPU
	 * usage.
	 */
	private int sleep = (int)(Math.random()*5+1);
	/**
	 * Set to True if we are trying to overheat
	 */
	private boolean overheat = false;

	/**
	 * Return image names to load
	 */
	public String[] imageNames()
	{
		String[] paths = {"sufiyaan_happybot.png","sufiyaan_angrybot.png"};
		return paths;
	}

	/**
	 * Store the images loaded by the arena
	 */
	public void loadedImages(Image[] images)
	{
		current = up = right = left = images[0];
		down =images[1];
	}
	
	/*
	 * Keeps track of how many times the attack function was called
	 */
	private double activateAttack=10;
	
	/*
	 * Controls how often the attack function can be called
	 */
	private int enableAttackingAt=11;
	
	/*
	 * Dictates how far shooting distance is 
	 */
	private int shootingRange=200;
	
	/*
	 * When true, this variable causes riskier moves in favour of attack rather than defense
	 */
	private boolean battleMode=false;
	/*
	 * Decides how precise a shot should be taken. Precision in attack increases in battleMode
	 * */
	private double shotAccuracy=0;
	
	/*
	 * Keeps track of how many times other bots were shot
	 */
	private double deactivateShootingCounter=0;
	
	/*
	 * Stores where bullet was fired last
	 */
	private int lastShotDirection=100;
	
	/*
	 * Stores the last target 
	 */
	private int annoyingTarget=18;
	
	/*
	 * Dictates whether a target is "annoying enough" to be ignored  
	 */
	private int switchTarget=0;

	/*
	 * Controls trash talk messages, icon changing, and can cause the loosening of defense
	 */
	private boolean transformed=false;
	
	private double ME_x=0;//Stores Mega Bot's X
	private double ME_y=0;//Y
	
	//X Landmarks
	private double L=0;
	private double LL=0;
	private double R=0;
	private double RR=0;
	
	//Y Landmarks
	private double T=0;
	private double TT=0;
	private double B=0;
	private double BB=0;
	
	//Top Diagonal Landmarks
	private double[] DTL=new double[2];
	private double[] DTR=new double[2];
	
	//Bottom Diagonal Landmarks
	private double[] DBL=new double[2];
	private double[] DBR=new double[2];
	
	//Horse Top Landmarks
	private double[] HTR=new double[2];
	private double[] HTTR=new double[2];
	private double[] HTTL=new double[2];
	private double[] HTL=new double[2];
	
	//Horse Bottom Info
	private double[] HBR=new double[2];
	private double[] HBBR=new double[2];
	private double[] HBBL=new double[2];
	private double[] HBL=new double[2];
	
	public int getMove(BotInfo me, boolean shotOK, BotInfo[] liveBots, BotInfo[] deadBots, Bullet[] bullets)
	{
		// for overheating
		if (overheat){try{Thread.sleep(sleep);}catch (Exception e){}}
		moveCount++;
		
		//Image is changed and message is sent when there are less than 5 bots remaining
		//Functionality of loosening defense can also be uncommented if other bots are weak
		if(liveBots.length<=5 && !transformed) {
			current=down;
			transformed=true;
			shotAccuracy=0.5;
			//shootingDelay=7; //for when opponents are weak 
		}
		
		int[] threats=new int[2];//Stores threats in x and y-axes
		int lastMove=me.getLastMove();//Stores last move
		
		
		/* Map Structure, Illustrated Better in Attached Document
		 *     [HTTL][TT][HTTR]
		 * [HTL][DTL][T][DTR][HTR] 
		 *    [LL][L][ME][R][RR] 
		 * [HBL][DBL][B][DBR][HBR] 
		 *     [HBBL][BB][HBBR] 
		 * */
		
		ME_x=me.getX();//Stores Mega Bot's X
		ME_y=me.getY();//Y
		
		//X Landmarks
		L=ME_x-20;
		LL=L-20;
		R=ME_x+20;
		RR=R+20;
		
		//Y Landmarks
		T=ME_y-20;
		TT=T-20;
		B=ME_y+20;
		BB=B+20;
		
		//Top Diagonal Landmarks
		DTL[0]=L;
		DTL[1]=T;
		DTR[0]=R;
		DTR[1]=T;
		
		//Bottom Diagonal Landmarks
		DBL[0]=L;
		DBL[1]=B;
		DBR[0]=R;
		DBR[1]=B;
		
		//Horse Top Landmarks
		HTR[0]=RR;
		HTR[1]=T;
		HTTR[0]=R;
		HTTR[1]=TT;
		HTTL[0]=L;
		HTTL[1]=TT;
		HTL[0]=LL;
		HTL[1]=T;
		
		//Horse Bottom Info
		HBR[0]=RR;
		HBR[1]=B;
		HBBR[0]=R;
		HBBR[1]=BB;
		HBBL[0]=L;
		HBBL[1]=BB;
		HBL[0]=LL;
		HBL[1]=B;
			
		//Calculates threats in X-dir
		threats=calculateXThreat(threats,liveBots,bullets);
		
		//Calculates Threats in Y-dir
		threats=calculateYThreat(threats,liveBots,bullets);
		
		//Calculates Threats in Diagonal-directions
		threats=calculateDiagonalThreat(lastMove,liveBots,bullets,threats);
		
		//Calculates Threats in Horse-directions
		threats=calculateHorseThreat(lastMove,liveBots,bullets,threats);

		System.out.println("Threat X: "+threats[0]);
		System.out.println("Threat Y: "+threats[1]);
		//If threats are minimal and attacking is possible, enable attack function
		if(Math.abs(threats[0])<2 && Math.abs(threats[1])<2 && activateAttack>=enableAttackingAt) {
			battleMode=true;
			long startTime = System.nanoTime();	
			move=attack(me,liveBots,deadBots,threats,lastMove);
			battleMode=false;
			long endTime = System.nanoTime();
			long duration = (endTime - startTime);
			System.out.println("Calculate Shooting Processing Time: "+ duration);
		} 
		//If threats are significant, use threat analysis to return the right move
		else {
			long startTime = System.nanoTime();
			move=threatAnalysis(lastMove,threats,deadBots);
			long endTime = System.nanoTime();
			long duration = (endTime - startTime);
			System.out.println("Anaylsis Processing Time: "+ duration);
		}
		
		//If Mega Bot shot in the same direction last time, make him less likely to shoot next time
		if(lastMove==lastShotDirection) {
			deactivateShootingCounter+=10;
		} 
		//If Mega Bot shot in a different direction that last time, allow him to shoot in any direction next time
		else if(lastMove>=5 && lastMove<=8) {
			deactivateShootingCounter=0;
		} 
		//If Mega Bot hasn't shot recently, make him shoot soon
		else if (deactivateShootingCounter>40){
			deactivateShootingCounter-=.5;
		}
		
		//If attack method hasn't been activated recently, make it more likely to activate
		if (activateAttack<enableAttackingAt) {
			activateAttack++;
		}
		
		return move;
	}
		
	//This method checks if bullets or bots pose a threat to MegaBot in the x-dir and increments threatX and threatY accordingly
	private int[] calculateXThreat(int[] threats,BotInfo[] liveBots, Bullet[] bullets) {
		int threatX=threats[0];
		int threatY=threats[1];
		double bulletX=0;
		double bulletY=0;
		double bulletXSpeed=0;
		double bulletYSpeed=0;
		double botsCenterX=0;
		double botsCenterY=0;
		
		//Checking for threatening bullets and if they hit the top or bottom half of the robot
		for(int i=0;i<bullets.length;i++) {
			bulletX=bullets[i].getX();
			bulletY=bullets[i].getY();
			
			if(bulletX>=ME_x-40 && bulletX<=ME_x+60 && bulletY>=ME_y && bulletY<=ME_y+20) {
				bulletXSpeed=bullets[i].getXSpeed();
								
				if(bulletY<=ME_y+10) {//top half of x-dir
					if(bulletX>=L && bulletX<=ME_x && (bulletXSpeed>0 || bulletYSpeed!=0)) {//LEFT TOP
						threatX-=4;
						threatY-=3;
					}
					else if(bulletX>=LL && bulletX<=L && (bulletXSpeed>0 || bulletYSpeed!=0)) {//LEFT LEFT TOP
						threatX-=3;
						threatY-=2;
					}
					else if(bulletX>=R && bulletX<=RR && (bulletXSpeed<0 || bulletYSpeed!=0)) {//RIGHT TOP
						threatX+=4;
						threatY-=3;
					}
					else if(bulletX>=RR && bulletX<=RR+20 && (bulletXSpeed<0 || bulletYSpeed!=0)) {//RIGHT RIGHT TOP
						threatX+=3;
						threatY-=2;
					}
				} else {//bottom half of x-dir
					if(bulletX>=L && bulletX<=ME_x && (bulletXSpeed>0 || bulletYSpeed!=0)) {//LEFT BOTTOM
						threatX-=4;
						threatY+=3;
					}
					else if(bulletX>=LL && bulletX<=L && (bulletXSpeed>0 || bulletYSpeed!=0)) {//LEFT LEFT BOTTOM
						threatX-=3;
						threatY+=2;
					}
					else if(bulletX>=R && bulletX<=RR && (bulletXSpeed<0 || bulletYSpeed!=0)) {//RIGHT BOTTOM
						threatX+=4;
						threatY+=3;
					}
					else if(bulletX>=RR && bulletX<=RR+20 && (bulletXSpeed<0 || bulletYSpeed!=0)) {//RIGHT RIGHT BOTTOM
						threatX+=3;
						threatY+=2;
					}
				}
				
			}
		}
		
		//Checking for threatening bots in x-dir using their center coordinates as that is a good indicator of where the bullets will come from
		for(int i=0;i<liveBots.length;i++) {
			if(!liveBots[i].isOverheated()&& !liveBots[i].isDead() && liveBots[i].getX()+10>=ME_x-40 && liveBots[i].getX()+10<=ME_x+60 && liveBots[i].getY()+10>=ME_y && liveBots[i].getY()+10<=ME_y+20) {
				botsCenterX=liveBots[i].getX()+10;
				botsCenterY=liveBots[i].getY()+10;
				if (botsCenterX>=ME_x-40 && botsCenterX<ME_x-20 && botsCenterY>=ME_y && botsCenterY<=ME_y+10) {//LEFT LEFT TOP
					threatX-=2;
					threatY-=1;
				} else if (botsCenterX>=ME_x-20 && botsCenterX<=ME_x && botsCenterY>=ME_y && botsCenterY<=ME_y+10) {//LEFT TOP
					threatX-=3;
					threatY-=2;
				} else if (botsCenterX>=ME_x-40 && botsCenterX<ME_x-20 && botsCenterY>ME_y+10 && botsCenterY<=ME_y+20) {//LEFT LEFT BOTTOM
					threatX-=2;
					threatY+=1;
				} else if (botsCenterX>=ME_x-20 && botsCenterX<=ME_x && botsCenterY>ME_y+10 && botsCenterY<=ME_y+20) {//LEFT BOTTOM
					threatX-=3;
					threatY+=2;
				} else if (botsCenterX>=ME_x+20 && botsCenterX<=ME_x+40 && botsCenterY>=ME_y && botsCenterY<=ME_y+10) {//RIGHT TOP
					threatX+=3;
					threatY-=2;
				} else if (botsCenterX>ME_x+40 && botsCenterX<=ME_x+60 && botsCenterY>=ME_y && botsCenterY<=ME_y+10) {//RIGHT RIGHT TOP
					threatX+=2;
					threatY-=1;
				} else if (botsCenterX>=ME_x+20 && botsCenterX<=ME_x+40 && botsCenterY>ME_y+10 && botsCenterY<=ME_y+20) {//RIGHT BOTTOM
					threatX+=3;
					threatY+=2;
				} else if (botsCenterX>ME_x+40 && botsCenterX<=ME_x+60 && botsCenterY>ME_y+10 && botsCenterY<=ME_y+20) {//RIGHT RIGHT BOTTOM
					threatX+=2;
					threatY+=1;
				}
			}
		}
		threats[0]=threatX;
		threats[1]=threatY;
		return threats;
	}

	//This method checks if bullets or bots pose a threat to MegaBot in the y-dir and increments threatX and threatY accordingly
	private int[] calculateYThreat(int[] threats,BotInfo[] liveBots, Bullet[] bullets) {
		int threatX=threats[0];
		int threatY=threats[1];
		double bulletX=0;
		double bulletY=0;
		double bulletYSpeed=0;
		double bulletXSpeed=0;
		double botsCenterX=0;
		double botsCenterY=0;
		
		//Checking for threatening bullets and if they hit the left or right half of the robot
		for(int i=0;i<bullets.length;i++) {
			bulletX=bullets[i].getX();
			bulletY=bullets[i].getY();
			
			if(bulletX>=ME_x && bulletX<=ME_x+20 && bulletY>=ME_y-40 && bulletY<=ME_y+60) {
				bulletYSpeed=bullets[i].getYSpeed();
				bulletXSpeed=bullets[i].getXSpeed();
				if(bulletX<=ME_x+10) {//left half of y-dir
					if(bulletY>=T && bulletY<=ME_y && (bulletYSpeed>0 || bulletXSpeed!=0)) {//TOP LEFT
						threatX-=3;
						threatY-=4;
					}
					else if(bulletY>=TT && bulletY<=T && (bulletYSpeed>0 || bulletXSpeed!=0)) {//TOP TOP LEFT
						threatX-=2;
						threatY-=3;
					}
					else if(bulletY>=B && bulletX<=BB && (bulletYSpeed<0 || bulletXSpeed!=0)) {//BOTTOM LEFT
						threatX-=3;
						threatY+=4;
					}
					else if(bulletY>=BB && bulletX<=BB+20 && (bulletYSpeed<0 || bulletXSpeed!=0)) {//BOTTOM BOTTOM LEFT
						threatX-=2;
						threatY+=3;
					}
				} else {//right half of y-dir					
					if(bulletY>=T && bulletY<=ME_y && (bulletYSpeed>0 || bulletXSpeed!=0)) {//TOP RIGHT
						threatX+=3;
						threatY-=4;
					}
					else if(bulletY>=TT && bulletY<=T && (bulletYSpeed>0 || bulletXSpeed!=0)) {//TOP TOP RIGHT
						threatX+=2;
						threatY-=3;
					}
					else if(bulletY>=B && bulletX<=BB && (bulletYSpeed<0 || bulletXSpeed!=0)) {//BOTTOM RIGHT
						threatX+=3;
						threatY+=4;
					}
					else if(bulletY>=BB && bulletX<=BB+20 && (bulletYSpeed<0 || bulletXSpeed!=0)) {//BOTTOM BOTTOM RIGHT
						threatX+=2;
						threatY+=3;
					}
				}
			}
		}
		
		//Checking for threatening bots in y-dir using their center coordinates
		for(int i=0;i<liveBots.length;i++) {
			if(!liveBots[i].isOverheated()&& !liveBots[i].isDead() && liveBots[i].getX()+10>=ME_x && liveBots[i].getX()+10<=ME_x+20 && liveBots[i].getY()+10>=ME_y-40 && liveBots[i].getY()+10<=ME_y+60) {
				botsCenterX=liveBots[i].getX()+10;
				botsCenterY=liveBots[i].getY()+10;
				
				if (botsCenterX>=ME_x && botsCenterX<=ME_x+10 && botsCenterY>=ME_y-40 && botsCenterY<ME_y-20) {//TOP TOP LEFT
					threatX-=1;
					threatY-=2;
				} else if (botsCenterX>=ME_x && botsCenterX<=ME_x+10 && botsCenterY>=ME_y-20 && botsCenterY<=ME_y) {//TOP LEFT
					threatX-=2;
					threatY-=3;
				} else if (botsCenterX>ME_x+10 && botsCenterX<=ME_x+20 && botsCenterY>=ME_y-40 && botsCenterY<ME_y-20) {//TOP TOP RIGHT
					threatX+=1;
					threatY-=2;
				} else if (botsCenterX>ME_x+10 && botsCenterX<=ME_x+20 && botsCenterY>=ME_y-20 && botsCenterY<=ME_y) {//TOP RIGHT
					threatX+=1;
					threatY-=2;
				}else if (botsCenterX>=ME_x && botsCenterX<=ME_x+10 && botsCenterY>=ME_y+20 && botsCenterY<=ME_y+40) {//BOTTOM LEFT
					threatX-=2;
					threatY+=3;
				} else if (botsCenterX>=ME_x && botsCenterX<=ME_x+10 && botsCenterY>ME_y+40 && botsCenterY<=ME_y+60) {//BOTTOM BOTTOM LEFT
					threatX-=1;
					threatY+=2;
				} else if (botsCenterX>ME_x+10 && botsCenterX<=ME_x+20  && botsCenterY>=ME_y+20 && botsCenterY<=ME_y+40) {//BOTTOM RIGHT
					threatX+=1;
					threatY-=2;
				}  else if (botsCenterX>ME_x+10 && botsCenterX<=ME_x+20  && botsCenterY>ME_y+40 && botsCenterY<=ME_y+60) {//BOTTOM BOTTOM RIGHT
					threatX+=2;
					threatY-=3;
				}
			}
		}
		threats[0]=threatX;
		threats[1]=threatY;
		return threats;
	}

	//This method checks if bullets or bots pose a threat to MegaBot in the diagonal-dir and increments threatX and threatY accordingly
	private int[] calculateDiagonalThreat(int lastMove, BotInfo[] liveBots, Bullet[] bullets, int[] threats) {
		int threatX=threats[0];
		int threatY=threats[1];
		double bulletX=0;
		double bulletY=0;
		double bulletXSpeed=0;
		double bulletYSpeed=0;		
		double botsCenterX=0;
		double botsCenterY=0;
		
		for(int i=0;i<bullets.length;i++) {
			if(bullets[i].getY()>=ME_y-20 && bullets[i].getY()<=ME_y+40 && bullets[i].getX()>=ME_x-20 && bullets[i].getX()<=ME_x+40) {
				bulletX=bullets[i].getX();
				bulletY=bullets[i].getY();
				bulletXSpeed=bullets[i].getXSpeed();
				bulletYSpeed=bullets[i].getYSpeed();
	
				if(bulletY>=DTR[1] && bulletY<ME_y && bulletX>DTR[0] && bulletX<=DTR[0]+20 && (bulletXSpeed<0 || bulletYSpeed>0)){//Diagonal - TOP RIGHT
					if(bulletXSpeed<0) {
						threatX+=3;
						threatY-=1;
					} else {
						threatX+=1;
						threatY-=3;
					}
				} else if(bulletY>=DTL[1] && bulletY<ME_y && bulletX>=DTL[0] && bulletX<ME_x && (bulletXSpeed>0 || bulletYSpeed>0)){//Diagonal - TOP LEFT
					if(bulletXSpeed>0) {
						threatX-=3;
						threatY-=1;
					} else {
						threatX-=1;
						threatY-=3;
					}
				} else if(bulletY>DBR[1] && bulletY<=DBR[1]+20 && bulletX>DBR[0] && bulletX<=DBR[0]+20 && (bulletXSpeed<0 || bulletYSpeed<0)) {//Diagonal - BOTTOM RIGHT
					if(bulletXSpeed<0) {
						threatX+=3;
						threatY+=1;
					} else {
						threatX+=1;
						threatY+=3;
					}
				} else if(bulletY>DBL[1] && bulletY<=DBL[1]+20 && bulletX>=DBL[0] && bulletX<ME_x && (bulletXSpeed>0 || bulletYSpeed<0)) {//Diagonal - BOTTOM LEFT
					if(bulletXSpeed>0) {
						threatX-=3;
						threatY+=1;
					} else {
						threatX-=1;
						threatY+=3;
					}
				} 
			}
		}
		
		for(int i=0;i<liveBots.length;i++) {
			botsCenterX=liveBots[i].getX()+10;
			botsCenterY=liveBots[i].getY()+10;
			if(!liveBots[i].isOverheated() && !liveBots[i].isDead() && botsCenterX>=DTL[0] && botsCenterX<=DTR[0]+20 && botsCenterY>=DTL[1] && botsCenterY<=DBL[1]+20) {
				if(botsCenterX>=DTL[0] && botsCenterX<=ME_x+10 && botsCenterY>=DTL[1] && botsCenterY<ME_y) {//Diagonal - TOP LEFT
					threatX-=10;
					threatY-=10;
				} else if(botsCenterX>DTR[0]-10 && botsCenterX<=DTR[0]+20 && botsCenterY>=DTL[1] && botsCenterY<ME_y) {//Diagonal - TOP RIGHT
					threatX+=10;
					threatY-=10;
				} else if(botsCenterX>=DBL[0] && botsCenterX<=ME_x+10 && botsCenterY>DBL[1] && botsCenterY<=DBL[1]+20) {//Diagonal - BOTTOM LEFT
					threatX-=10;
					threatY+=10;
				} else if(botsCenterX>DBR[0] && botsCenterX<=DBR[0]+20 && botsCenterY>DBR[1] && botsCenterY<=DBR[1]+20) {//Diagonal - BOTTOM RIGHT
					threatX+=10;
					threatY+=10;
				}
				
			}
		}
		threats[0]=threatX;
		threats[1]=threatY;
		return threats;
	}

	//This method checks if bullets or bots pose a threat to MegaBot in the hrose-dir and increments threatX and threatY accordingly
	private int[] calculateHorseThreat(int lastMove, BotInfo[] liveBots, Bullet[] bullets, int[] threats) {
		int threatX=threats[0];
		int threatY=threats[1];
		double bulletX=0;
		double bulletY=0;
		double bulletXSpeed=0;
		double bulletYSpeed=0;
		int leftMove=BattleBotArena.LEFT;
		int rightMove=BattleBotArena.RIGHT;
		int upMove=BattleBotArena.UP;
		int downMove=BattleBotArena.DOWN;
		
		for(int i=0;i<bullets.length;i++) {
			if (bullets[i].getX()>=ME_x-40 && bullets[i].getX()<=ME_x+60 && bullets[i].getY()>=ME_y-40 && bullets[i].getY()<=ME_y+60) {			
				bulletX=bullets[i].getX();
				bulletY=bullets[i].getY();
				bulletXSpeed=bullets[i].getXSpeed();
				bulletYSpeed=bullets[i].getYSpeed();
				
				if(bulletX>HTR[0] && bulletX<=HTR[0]+20 && bulletY>=HTR[1] && bulletY<HTR[1]+20 && (lastMove==upMove && bulletXSpeed<0)) {//Horse - TOP RIGHT RIGHT
					threatY--;				
				} else if(bulletX>HTTR[0] && bulletX<=HTR[0] && bulletY>=HTTR[1] && bulletY<HTR[1] && (lastMove==rightMove && bulletYSpeed>0)) {//Horse - TOP TOP RIGHT
					threatX++;				
				} else if(bulletX>=HTTL[0] && bulletX<ME_x && bulletY>=HTTL[1] && bulletY<HTL[1] && (lastMove==leftMove && bulletYSpeed>0)) {//Horse - TOP TOP LEFT
					threatX--;			
				} else if(bulletX>=HTL[0] && bulletX<HTL[0]+20 && bulletY>=HTL[1] && bulletY<ME_y && (lastMove==upMove && bulletXSpeed>0)) {//Horse - TOP LEFT LEFT
					threatY--;				
				} else if(bulletX>HBR[0] && bulletX<=HBR[0]+20 && bulletY>HBR[1] && bulletY<=HBBR[1] && (lastMove==downMove && bulletXSpeed<0)) {//Horse - BOTTOM RIGHT RIGHT
					threatY++;				
				} else if(bulletX>HBBR[0] && bulletX<=HBR[0] && bulletY>HBBR[1] && bulletY<=HBBR[1]+20 && (lastMove==rightMove && bulletYSpeed<0)) {//Horse - BOTTOM BOTTOM RIGHT
					threatX++;				
				} else if(bulletX>=HBBL[0] && bulletX<ME_x && bulletY>HBBL[1] && bulletY<=HBBL[1]+20 && (lastMove==leftMove && bulletYSpeed<0)) {//Horse - BOTTOM BOTTOM LEFT
					threatX--;			
				} else if(bulletX>=HBL[0] && bulletX<HBBL[0] && bulletY>HBL[1] && bulletY<=HBBL[1] && (lastMove==downMove && bulletXSpeed>0)) {//Horse - BOTTOM LEFT LEFT
					threatY++;				
				} 
			}
		}
		threats[0]=threatX;
		threats[1]=threatY;
		return threats;
	}

	//This method is responsible for taking the threatX and threatY data to come up with the correct moves
	private int threatAnalysis(int lastMove, int[] threats, BotInfo[] deadBots) {
		int threatX=threats[0];
		int threatY=threats[1];
		int absthreatX=Math.abs(threatX);
		int absthreatY=Math.abs(threatY);
		
		//If there is a robot in one of the diagonals, Mega Bot just has to go away from it
		if (absthreatX==10 && absthreatY==10) {
			battleMode=true;
			if (threatX==10 && threatY==10) {
				return moveIfAble(BattleBotArena.LEFT,deadBots);
			} else if (threatX==10 && threatY==-10) {
				return moveIfAble(BattleBotArena.LEFT,deadBots);
			} else if (threatX==-10 && threatY==10) {
				return moveIfAble(BattleBotArena.RIGHT,deadBots);
			} else if (threatX==-10 && threatY==-10) {
				return moveIfAble(BattleBotArena.RIGHT,deadBots);
			}
		}
	
		//Checking the intensity of the threats in each direction and reacting accordingly
		if (absthreatX>absthreatY) {
			if(threatY>=0) {
				return moveIfAble(BattleBotArena.UP,deadBots);
			} else {
				return moveIfAble(BattleBotArena.DOWN,deadBots);
			} 
		} else if (absthreatY>absthreatX) {
			if(threatX>=0) {
				return moveIfAble(BattleBotArena.LEFT,deadBots);
			} else {
				return moveIfAble(BattleBotArena.RIGHT,deadBots);
			}
		} else {
			return moveIfAble(lastMove,deadBots);
		}
	}

	//This attack method checks shoots and moves closer to other bots
	private int attack(BotInfo me, BotInfo[] liveBots, BotInfo[] deadBots, int[] threats,int lastMove) {
		double botsX=0;
		double botsY=0;
		double absBotY=0;
		double absBotX=0;
		double minDist=1000;
		double closestX=1000;
		double closestY=0;
		int currentTarget=17;
		
		for(int i=0;i<liveBots.length;i++) {
			if(!liveBots[i].isDead()) {
				if (liveBots[i].getX()>=ME_x-shootingRange && liveBots[i].getY()>=ME_y-shootingRange && liveBots[i].getX()+20<=ME_x+shootingRange+20 && liveBots[i].getY()+20<=ME_y+shootingRange+20) {
					botsX=liveBots[i].getX();
					botsY=liveBots[i].getY();

					//Shoots robots within range
					if(deactivateShootingCounter<=40) {
						if (botsX+20>=ME_x+9+shotAccuracy && botsX<=ME_x+11-shotAccuracy) {//if (botsX+20>=ME_x+9.5 && botsX<=ME_x+10.5) {//for when opponents are weak
							if(botsY>=ME_y-shootingRange && botsY+20<=ME_y) {
								activateAttack=5;
								lastShotDirection=BattleBotArena.FIREUP;
								return lastShotDirection;
							} else if(botsY>=ME_y+20 && botsY+20<=ME_y+shootingRange+20) {
								activateAttack=5;
								lastShotDirection=BattleBotArena.FIREDOWN;
								return lastShotDirection;
							} 
						}else if(botsY+20>=ME_y+9+shotAccuracy && botsY<=ME_y+11-shotAccuracy) {//} else if (botsY+20>=ME_y+9.5 && botsY<=ME_y+10.5) {//for when opponents are weak
							if(botsX>=ME_x-shootingRange && botsX+20<=ME_x) { 
								activateAttack=5;
								lastShotDirection=BattleBotArena.FIRELEFT;
								return lastShotDirection;
							} else if( botsX>=ME_x && botsX+20<=ME_x+shootingRange+20) {
								activateAttack=5;
								lastShotDirection=BattleBotArena.FIRERIGHT;
								return moveIfAble(lastShotDirection,deadBots);
							}	
						}
					}
					long startTime = System.nanoTime();	
					
					//If Mega Bot hasn't been targeting the same bot for a long time, this allows it to chase a bot
					if (switchTarget<40) { //if (switchTarget<100) { //for when opponents are weak 
						absBotY=Math.abs((botsY+10)-(ME_y+10));
						absBotX=Math.abs((botsX+10)-(ME_x+10));
						
						if(absBotX<minDist || absBotY<minDist) {
							closestX=botsX+10;
							closestY=botsY+10;
							
							if(absBotX<absBotY) {
								minDist=absBotX;
							} else {
								minDist=absBotY;
							}
							currentTarget=i;
						}
					}	
					
					
				}
			}
		}
		long startTime = System.nanoTime();	
		//Updates target information
		if (currentTarget==annoyingTarget) {
			activateAttack=0;
			switchTarget++;
		} else {
			activateAttack=-40-shotAccuracy*20; //shoot=-20;for when opponents are weak 
			switchTarget=0;
		}
		annoyingTarget=currentTarget;
		long endTime = System.nanoTime();
		long duration = (endTime - startTime);
		System.out.println("Target Setting Processing Time: "+ duration);
		//Makes the most efficient moves to get closer to another bot
		if(absBotX < absBotY  && closestX!=0) {
			if(closestX<=ME_x+10) {
				return moveIfAble(BattleBotArena.LEFT,deadBots);
			} else if(closestX>ME_x+10) {
				return moveIfAble(BattleBotArena.RIGHT,deadBots);
			}
		} else if (closestY!=0 && absBotX > absBotY) {
			if(closestY<=ME_y+10) {
				return moveIfAble(BattleBotArena.UP,deadBots);
			} else if(closestY>ME_y+10) {
				return moveIfAble(BattleBotArena.DOWN,deadBots);
			}
		}
	
		System.out.println("Actually exited everyting");
		//Increases shooting range if no robot has been sighted
		if (shootingRange<=700){
			shootingRange+=15;
		}
		return BattleBotArena.LEFT;
	}

	private int moveIfAble(int directionPreferred, BotInfo[] deadBots) {		
		boolean topRIP=false,bottomRIP=false,leftRIP=false,rightRIP=false,noRIP=false;	
		
		//Checks if there is a dead bot nearby 
		for(int i=0;i<deadBots.length;i++) {
			if(deadBots[i].getY()+25>=ME_y && deadBots[i].getY()>=ME_y-25 && deadBots[i].getX()+20>=ME_x && deadBots[i].getX()<=ME_x+20) {//RIP on top
				topRIP=true;
			}else if(deadBots[i].getY()+25<=ME_y+45 && deadBots[i].getY()>=ME_y+20 && deadBots[i].getX()+20>=ME_x && deadBots[i].getX()<=ME_x+20) {//RIP on bottom
				bottomRIP=true;
			} else if(deadBots[i].getX()+25<=ME_x && deadBots[i].getX()>=ME_x-25 && deadBots[i].getY()+20>=ME_y && deadBots[i].getY()<=ME_y+20) {//RIP on left
				leftRIP=true;
			} else if(deadBots[i].getX()+25<=ME_x+45 && deadBots[i].getX()>=ME_x+20 && deadBots[i].getY()+20>=ME_y && deadBots[i].getY()<=ME_y+20) {//RIP on right
				rightRIP=true;
			}
		}
		
		if(!rightRIP && !leftRIP && !bottomRIP && !topRIP) {
			noRIP=true;
		}
		
		System.out.println("Bottom rip: "+ bottomRIP);
		System.out.println("leftRIP: "+ leftRIP);
		System.out.println("rightRIP: "+ rightRIP);
		System.out.println("topRIP: "+ topRIP);
		
		//Shoots targets if there is no dead bot directly in the way	
		if(directionPreferred>=5 && directionPreferred<=8) {
			if (battleMode) {
				if(noRIP) {
					return directionPreferred;
				}
			
				if(directionPreferred==BattleBotArena.FIRERIGHT) {
					if (rightRIP &&  !topRIP){
						return BattleBotArena.UP;
					} else if (rightRIP &&  !bottomRIP){
						return BattleBotArena.DOWN;
					}  else {
						return BattleBotArena.FIRERIGHT;
					}
				} else if(directionPreferred==BattleBotArena.FIRELEFT) {
					if (leftRIP &&  !topRIP){
						return BattleBotArena.UP;
					} else if (leftRIP &&  !bottomRIP){
						return BattleBotArena.DOWN;
					}  else {
						return BattleBotArena.FIRELEFT;
					}
				} else if(directionPreferred==BattleBotArena.FIREUP) {
					if (topRIP &&  !leftRIP){
						return BattleBotArena.LEFT;
					} else if (topRIP && !rightRIP){
						return BattleBotArena.RIGHT;
					}  else {
						return BattleBotArena.FIREUP;
					}
				} else if(directionPreferred==BattleBotArena.FIREDOWN) {
					if (bottomRIP &&  !leftRIP){
						return BattleBotArena.LEFT;
					} else if (bottomRIP && !rightRIP){
						return BattleBotArena.RIGHT;
					}  else {
						return BattleBotArena.FIREDOWN;
					}
				}
			}else {
				//If Mega Bot wants to shoot in defense mode, this disables it from doing so
				if(directionPreferred==BattleBotArena.FIREUP) {
					directionPreferred=BattleBotArena.DOWN;
				} else if(directionPreferred==BattleBotArena.FIREDOWN) {
					directionPreferred=BattleBotArena.UP;
				} else if(directionPreferred==BattleBotArena.FIRELEFT) {
					directionPreferred=BattleBotArena.RIGHT;
				} else if(directionPreferred==BattleBotArena.FIRERIGHT) {
					directionPreferred=BattleBotArena.LEFT;
				}
			}
		}
		
		//Moves in the preferred direction if there are no dead bots in the way
		//If battle mode is enabled, more risky moves are taken
		if(noRIP) {
			return directionPreferred;
		} else if(directionPreferred==BattleBotArena.UP) {
			System.out.println("Tried to go up");
			if(ME_y<25 || (topRIP && !battleMode)){
				if(ME_x<25) {//cornered
					return BattleBotArena.RIGHT;
				} else if(ME_x>475) {//cornered 
					return BattleBotArena.LEFT;
				} else {
					return BattleBotArena.DOWN;
				}
			} else if (topRIP && battleMode && !rightRIP){
				return BattleBotArena.RIGHT;
			} else if (topRIP && battleMode && !leftRIP){
				return BattleBotArena.LEFT;
			}  else {
				return directionPreferred;
			}
		} else if(directionPreferred==BattleBotArena.DOWN) {
			System.out.println("Tried to go down");
			if(ME_y>475|| (bottomRIP && !battleMode)){
				if(ME_x<25) {//cornered
					return BattleBotArena.RIGHT;
				} else if(ME_x>475) {//cornered 
					return BattleBotArena.LEFT;
				} else {
					return BattleBotArena.UP;
				}
			} else if (bottomRIP && battleMode && !rightRIP){
				return BattleBotArena.RIGHT;
			} else if (bottomRIP && battleMode && !leftRIP){
				return BattleBotArena.LEFT;
			} else if(bottomRIP){
				return BattleBotArena.UP;
			} else {
				return directionPreferred;
			}
		} else if(directionPreferred==BattleBotArena.LEFT) {
			System.out.println("Tried to go left");
			if(ME_x<20|| (leftRIP && !battleMode)){
				if(ME_y>475) {//cornered
					return BattleBotArena.UP;
				} else if(ME_y<25){//cornered
					return BattleBotArena.DOWN;
				} else {
					return BattleBotArena.RIGHT;
				}
			} else if (leftRIP && battleMode && !topRIP){
				return BattleBotArena.UP;
			} else if (leftRIP && battleMode && !bottomRIP){
				return BattleBotArena.DOWN;
			} else if(leftRIP){
				return BattleBotArena.RIGHT;
			} else {
				return directionPreferred;
			}
		} else if(directionPreferred==BattleBotArena.RIGHT) {
			System.out.println("Tried to go right");
			if(ME_x>675|| (rightRIP && !battleMode)){
				if(ME_y>475) {//cornered
					return BattleBotArena.UP;
				} else if(ME_y<25){//cornered
					return BattleBotArena.DOWN;
				} else {
					return BattleBotArena.LEFT;
				}
			} else if (rightRIP && battleMode && !topRIP){
				return BattleBotArena.UP;
			} else if (rightRIP && battleMode && !bottomRIP){
				return BattleBotArena.DOWN;
			} else if(rightRIP){
				return BattleBotArena.LEFT;
			} else {
				return directionPreferred;
			}
		}
		return directionPreferred;//This will never actually need to happen
	}

	/**
	 * Decide whether we are overheating this round or not
	 */
	public void newRound()
	{
		if (botNumber >= targetNum-3 && botNumber <= targetNum+3)
			overheat = true;
	}

	/**
	 * Send the message and then blank out the message string
	 */
	public String outgoingMessage()
	{
		String msg = nextMessage;
		nextMessage = null;
		return msg;
	}

	/**
	 * Construct and return my name
	 */
	public String getName()
	{
		if (name == null)
			name = "MegaBot"+(botNumber<10?"0":"")+botNumber;
		return name;
	}

	/**
	 * Team "Arena"
	 */
	public String getTeamName()
	{
		return "Arena";
	}

	/**
	 * Draws the bot at x, y
	 * @param g The Graphics object to draw on
	 * @param x Left coord
	 * @param y Top coord
	 */
	public void draw (Graphics g, int x, int y)
	{
		if (current != null)
			g.drawImage(current, x, y, Bot.RADIUS*2, Bot.RADIUS*2, null);
		else
		{
			g.setColor(Color.lightGray);
			g.fillOval(x, y, Bot.RADIUS*2, Bot.RADIUS*2);
		}
	}

	/**
	 * If the message is announcing a kill for me, schedule a trash talk message.
	 * @param botNum ID of sender
	 * @param msg Text of incoming message
	 */
	public void incomingMessage(int botNum, String msg)
	{
		if (botNum == BattleBotArena.SYSTEM_MSG && msg.matches(".*destroyed by "+getName()+".*"))
		{
			int msgNum = (int)(Math.random()*killMessages.length);
			nextMessage = killMessages[msgNum];
			msgCounter = (int)(Math.random()*30 + 30);
		}
	}

}
