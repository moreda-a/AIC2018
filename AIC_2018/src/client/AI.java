package client;

import client.model.*;
import common.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;

import static common.network.JsonSocket.TAG;

/**
 * AI class. You should fill body of the method {@link }. Do not change name or
 * modifiers of the methods or fields and do not add constructor for this class.
 * You can add as many methods or fields as you want! Use world parameter to
 * access and modify game's world! See World interface for more details.
 */

// ISSUE [][] not working ... y x instead of x y
// TODO BEAN AND STORM WRONG MAP CHECK ... isMyMap
public class AI {

	public boolean hasShortPath;
	public int shortestPath;
	public Random rnd = new Random();

	public World world;
	public int turn;
	public Player my_player;
	public Player en_player;
	public Map at_map;
	public Map de_map;
	// public Cell[][] at_cells;
	// public Cell[][] de_cells;
	public int lu_cost;
	public double lu_hp;
	public double accHp_h;
	public double accHp_l;
	public int lu_count = 0;
	public int hu_cost;
	public double hu_hp;
	public int hu_count = 0;
	public PointVec[] toweri_c = new PointVec[120];
	public PointVec[] toweri_a = new PointVec[120];
	public int all_ct = 0;
	public int all_at = 0;

	public double[] path_power_c = new double[200];
	public double[] path_power_a = new double[200];

	public boolean firstTime = true;
	public HashMap<Point, Integer> towers_path = new HashMap<Point, Integer>();
	public HashMap<Integer, Point> id_to_point = new HashMap<Integer, Point>();
	public Point[][] allPoint = new Point[200][200];

	public int spendedMoney;
	public int width;
	public int height;
	public int money;
	public int at_money;
	public int[][][] good_for_tower;
	public ArrayList<Path> at_paths;
	public ArrayList<Path> de_paths;
	public int path_num;
	public int max_length_path = 0;
	public int min_length_path = 10000;
	public int[] path_sol_h;
	public int[] path_sol_l;
	public int[] path_tower_c = new int[100];
	public int[] path_tower_a = new int[100];
	// WTF WRONG

	public boolean[][] deadCell = new boolean[200][200];

	private boolean[][] notNearBuild = new boolean[200][200];
	// WTF WRONG

	public int[] four_dir_x = new int[] { 0, 1, 0, -1 };
	public int[] four_dir_y = new int[] { 1, 0, -1, 0 };

	public int[] tower_range_array_x = new int[] { 0, 1, 1, 1, 0, -1, -1, -1, 0, 2, 0, -2 };
	public int[] tower_range_array_y = new int[] { 1, 1, 0, -1, -1, -1, 0, 1, 2, 0, -2, 0 };
	int bean;
	int storm;

	private void doFirst(World world) {
		bean = world.getMyInformation().getBeansLeft();
		storm = world.getMyInformation().getStormsLeft();

		lu_cost = LightUnit.INITIAL_PRICE;
		lu_hp = (double) LightUnit.INITIAL_HEALTH;
		hu_cost = HeavyUnit.INITIAL_PRICE;
		hu_hp = (double) HeavyUnit.INITIAL_HEALTH;
		for (int i = 0; i < 200 * 200; ++i)
			allPoint[i / 200][i % 200] = new Point(i / 200, i % 200);
		for (int i = 0; i < 100; ++i) {
			path_tower_c[i] = 0;
			path_tower_a[i] = 0;
		}
		for (int i = 0; i < 200 * 200; ++i)
			deadCell[i / 200][i % 200] = false;
		for (int i = 0; i < 120; ++i) {
			toweri_c[i] = new PointVec();
			toweri_a[i] = new PointVec();
		}
		for (int i = 0; i < 200; ++i) {
			path_power_a[i] = 0;
			path_power_c[i] = 0;
		}
		int pi = 0;
		for (Path p : world.getAttackMapPaths()) {
			max_length_path = Math.max(max_length_path, p.getRoad().size());
			if (min_length_path > p.getRoad().size()) {
				min_length_path = p.getRoad().size();
				shortestPath = pi;
			}
			pi++;
		}
		if (min_length_path <= 26)
			hasShortPath = true;
		else
			hasShortPath = false;
		System.out.println("first shit: " + min_length_path);
	}

	void complexTurn(World game) {
		Log.d(TAG, "HeavyTurn Called" + " Turn:" + game.getCurrentTurn());
		simpleTurn(game);
	}

	boolean nextTurn = false;

	void simpleTurn(World game) {
		Log.d(TAG, "lightTurn Called" + " Turn:" + game.getCurrentTurn());
		// if (true)
		// return;
		update(game);
		shitHappend();
		attack();
		defence();
		destroyTowers();
		bombThem();
		// System.out.println("SS: " + world.getDefenceMap().getCell(1, 1));
		// System.out.println(world.getAttackMap().getCell(1, 1));
		// world.plantBean(1, 1);
		// System.out.println("SS: " + world.getDefenceMap().getCell(1, 1));
		// System.out.println(world.getAttackMap().getCell(1, 1));
		System.out.println("End Turn: " + spendedMoney);
	}

	private void update(World game) {
		if (firstTime) {
			doFirst(game);
			firstTime = false;
		}
		world = game;
		turn = world.getCurrentTurn();
		my_player = world.getMyInformation();
		en_player = world.getEnemyInformation();
		at_map = world.getAttackMap();
		de_map = world.getDefenceMap();
		// at_cells = at_map.getCellsGrid();
		// de_cells = de_map.getCellsGrid();
		width = at_map.getWidth();
		height = at_map.getHeight();
		// w,h EQ in both map
		at_paths = world.getAttackMapPaths();
		de_paths = world.getDefenceMapPaths();
		path_num = at_paths.size();
		path_sol_h = new int[path_num];
		path_sol_l = new int[path_num];
		// path EQ in both map
		good_for_tower = new int[path_num][width][height];
		spendedMoney = 0;
		money = my_player.getMoney();
		at_money = (money > 200 ? money / 2 : 0);
		if (turn > 250) {
			at_money = (money > 400 ? money / 2 : money - 200);
		}
		if (turn < 12) {
			if (path_num - storm + bean >= 1 && path_num - storm + bean <= 9)
				at_money = (money > 200 ? Math.min(money / 2, money - (path_num - storm + bean) * 135) : 0);
			else
				at_money = money;
			System.out.println(money - at_money);
		}
		for (int i = 0; i < 200 * 200; ++i)
			notNearBuild[i / 200][i % 200] = false;
		for (Tower tw : world.getDestroyedTowersInThisTurn()) {
			if (tw.getOwner() == Owner.ME)
				deadCell[tw.getLocation().getX()][tw.getLocation().getY()] = true;
		}
		id_to_point = new HashMap<Integer, Point>();
		for (Tower tw : world.getMyTowers()) {
			id_to_point.put(tw.getId(), tw.getLocation());
		}

		System.out.println("Turn: " + turn + " Money: " + world.getMyInformation().getMoney() + " cry: " + hu_count
				+ " - " + lu_count);
	}

	public int yix = 0;

	private void shitHappend() {
		// Check ? ?
		for (Tower tw : world.getDestroyedTowersInThisTurn()) {
			if (tw.getOwner() == Owner.ME) {
				Point p = allPoint[tw.getLocation().getX()][tw.getLocation().getY()];
				if (towers_path.containsKey(p)) {
					yix++;
					if (tw.getClass() == ArcherTower.class) {
						System.out.println("SHITTTTTTTTTTTTTTTTTTTTT :" + toweri_a[towers_path.get(p)].remove(p));
						// toweri_a[towers_path.get(p)].remove(p);
						--path_tower_a[towers_path.get(p)];
						path_power_a[towers_path.get(p)] -= ArcherTower.INITIAL_DAMAGE
								* Math.pow(ArcherTower.DAMAGE_COEFF, tw.getLevel() - 1);
					} else {
						System.out.println("SHITTTTTTTTTTTTTTTTTTTTT :" + toweri_c[towers_path.get(p)].remove(p));
						--path_tower_c[towers_path.get(p)];
						path_power_c[towers_path.get(p)] -= CannonTower.INITIAL_DAMAGE
								* Math.pow(CannonTower.DAMAGE_COEFF, tw.getLevel() - 1);
					}
				} else {
					System.out.println("GETTING OUT OF MY GAME");
					System.out.println(towers_path.entrySet());
				}
			}
		}
		if (world.getMyTowers().size() != all_at + all_ct - yix)
			System.out.println(
					"NO PLS NO NOT NOW" + world.getMyTowers().size() + " - " + (all_at + all_ct) + " - " + yix);
	}

	// in short path map eq but shoul attack
	private void attack() {
		int cr = at_money / LightUnit.INITIAL_PRICE;
		int tr = -1;
		// what about use shortest path ? PLS? TODO
		if (((hasShortPath && my_player.getStrength() != en_player.getStrength())
				|| my_player.getStrength() < en_player.getStrength())
				&& 1000 - turn < (((LightUnit.INITIAL_BOUNTY + LightUnit.INITIAL_PRICE) / LightUnit.ADDED_INCOME)
						+ (((LightUnit.BOUNTY_INCREASE + LightUnit.PRICE_INCREASE) / LightUnit.ADDED_INCOME)
								* (lu_count / LightUnit.LEVEL_UP_THRESHOLD)))
						* 10
				&& 1000 - turn > min_length_path * 4) {
			// no return for making noob unit's
			// but check if we are equal keep attack to make money
			if (1000 - turn == min_length_path * 4 + 20) {
				cr = (2 * money) / (9 * LightUnit.INITIAL_PRICE);
				while (cr-- >= 0 && (2 * money) / 9 >= spendedMoney) {
					tr = (rnd.nextInt() & Integer.MAX_VALUE) % path_num;
					if (money - spendedMoney >= lu_cost) {
						world.createLightUnit(tr);
						spendedMoney += lu_cost;
						++lu_count;
						if (lu_count % LightUnit.LEVEL_UP_THRESHOLD == 0) {
							lu_hp *= LightUnit.HEALTH_COEFF;
							lu_cost += LightUnit.PRICE_INCREASE;
						}
					}
				}
				cr = money / (9 * HeavyUnit.INITIAL_PRICE);
				while (cr-- >= 0 && money / 3 >= spendedMoney) {
					tr = (rnd.nextInt() & Integer.MAX_VALUE) % path_num;
					if (/* turn > 30 && */ money - spendedMoney >= hu_cost) {
						world.createHeavyUnit(tr);
						spendedMoney += hu_cost;
						++hu_count;
						if (hu_count % HeavyUnit.LEVEL_UP_THRESHOLD == 0) {
							hu_hp *= HeavyUnit.HEALTH_COEFF;
							hu_cost += HeavyUnit.PRICE_INCREASE;
						}

					}
				}

			} else if (1000 - turn == min_length_path * 4 + 10) {
				cr = money / (3 * LightUnit.INITIAL_PRICE);
				while (cr-- >= 0 && money / 3 >= spendedMoney) {
					tr = shortestPath;
					if (money - spendedMoney >= lu_cost) {
						world.createLightUnit(tr);
						spendedMoney += lu_cost;
						++lu_count;
						if (lu_count % LightUnit.LEVEL_UP_THRESHOLD == 0) {
							lu_hp *= LightUnit.HEALTH_COEFF;
							lu_cost += LightUnit.PRICE_INCREASE;
						}
					}
				}
				cr = money / (HeavyUnit.INITIAL_PRICE);
				while (cr-- >= 0 && money >= spendedMoney) {
					tr = shortestPath;
					if (/* turn > 30 && */ money - spendedMoney >= hu_cost) {
						world.createHeavyUnit(tr);
						spendedMoney += hu_cost;
						++hu_count;
						if (hu_count % HeavyUnit.LEVEL_UP_THRESHOLD == 0) {
							hu_hp *= HeavyUnit.HEALTH_COEFF;
							hu_cost += HeavyUnit.PRICE_INCREASE;
						}
					}
				}
			}
		} else {
			// spend all at_money on unit we build some sort of shit unit all time but not
			// to much
			while (cr-- >= 0) {
				if ((double) LightUnit.ADDED_INCOME / lu_cost > (double) HeavyUnit.ADDED_INCOME / hu_cost) {
					tr = (rnd.nextInt() & Integer.MAX_VALUE) % path_num;
					if (money - spendedMoney >= lu_cost) {
						world.createLightUnit(tr);
						spendedMoney += lu_cost;
						++lu_count;
						if (lu_count % LightUnit.LEVEL_UP_THRESHOLD == 0) {
							lu_hp *= LightUnit.HEALTH_COEFF;
							lu_cost += LightUnit.PRICE_INCREASE;
						}
					}
				} else {
					tr = (rnd.nextInt() & Integer.MAX_VALUE) % path_num;
					if (/* turn > 30 && */ money - spendedMoney >= hu_cost) {
						world.createHeavyUnit(tr);
						spendedMoney += hu_cost;
						++hu_count;
						if (hu_count % HeavyUnit.LEVEL_UP_THRESHOLD == 0) {
							hu_hp *= HeavyUnit.HEALTH_COEFF;
							hu_cost += HeavyUnit.PRICE_INCREASE;
						}
					}
				}
			}
		}
		// END GAME

	}

	private void destroyTowers() {
		// no strategy kill as soon as you see tower first three towers
		if (my_player.getBeansLeft() == 0)
			return;
		int dl = 0;
		for (Tower tw : world.getVisibleEnemyTowers()) {
			if (my_player.getBeansLeft() - dl != 0) {
				world.plantBean(tw.getLocation().getX(), tw.getLocation().getY());
				++dl;
			}
		}
	}

	private void defence() {
		ArrayList<Integer> shuff = new ArrayList<Integer>();
		for (int i = 0; i < path_num; ++i)
			shuff.add(i);
		Collections.shuffle(shuff);
		for (Integer pi : shuff) {
			Path p = de_paths.get(pi);
			accHp_h = 0;
			accHp_l = 0;
			path_sol_h[pi] = 0;
			path_sol_l[pi] = 0;

			// i have better idea by first distinct point(maybe not exist)
			for (RoadCell rc : p.getRoad()) {
				for (Unit u : rc.getUnits()) {
					if (u.getClass() == HeavyUnit.class) {
						++path_sol_h[pi];
						accHp_h += (double) (Math.pow(HeavyUnit.HEALTH_COEFF, u.getLevel() - 1)
								* HeavyUnit.INITIAL_HEALTH);
					} else {
						++path_sol_l[pi];
						accHp_l += (double) (Math.pow(LightUnit.HEALTH_COEFF, u.getLevel() - 1)
								* LightUnit.INITIAL_HEALTH);
					}
				}
			}
			if (turn % 10 == 1)
				System.out.println("Pi: " + pi + " sl " + accHp_l + " : " + (path_sol_l[pi] * 32) + " sh " + accHp_h
						+ " : " + path_sol_h[pi] * 240 + " tc " + path_power_c[pi] + " : " + (path_tower_c[pi] * 10)
						+ " ta " + path_power_a[pi] + " : " + (path_tower_a[pi] * 60));
			double d1 = (double) (1 / 2) * path_power_c[pi] * (path_sol_l[pi] + 2 * path_sol_h[pi]);
			double d2 = (double) 3 * path_power_a[pi];
			double c1 = (double) 5 * (path_sol_l[pi] + 2 * path_sol_h[pi])
					/ (CannonTower.INITIAL_PRICE + CannonTower.INITIAL_PRICE_INCREASE * all_ct);
			double c2 = 3 * 60 / (ArcherTower.INITIAL_PRICE + ArcherTower.INITIAL_PRICE_INCREASE * all_at);
			if (accHp_l + accHp_h > d1 + d2 && (turn > 100 || path_num < 4 || accHp_l + accHp_h > d1 + d2 + 300
					|| path_tower_a[pi] == 0 || all_at - bean + en_player.getBeansLeft() >= path_num - 1)) {
				if (c1 >= c2
						&& money - spendedMoney >= CannonTower.INITIAL_PRICE
								+ CannonTower.INITIAL_PRICE_INCREASE * all_ct
						&& (my_player.getStormsLeft() == 0 || turn >= 10)
				/* && (turn > min_length_path || turn == 10) */) {
					// upgrade safe cannons?!
					// no more room ?! and strong weak unit's
					Point p0 = nextTower(p, pi);
					if (path_tower_c[pi] >= 1 && (p0 == null || lastBest <= 2 || all_ct > 16)) {
						System.out.println("UPC" + p0.getX() + " - " + p0.getY());
						int tr = (rnd.nextInt() & Integer.MAX_VALUE) % toweri_c[pi].size();
						Tower tt = ((GrassCell) de_map.getCell(toweri_c[pi].get(tr).getX(),
								toweri_c[pi].get(tr).getY())).getTower();
						if (money - spendedMoney >= CannonTower.INITIAL_LEVEL_UP_PRICE
								* Math.pow(CannonTower.PRICE_COEFF, tt.getLevel() - 1)) {
							world.upgradeTower(tt);
							spendedMoney += CannonTower.INITIAL_LEVEL_UP_PRICE
									* Math.pow(CannonTower.PRICE_COEFF, tt.getLevel() - 1);
							path_power_c[pi] += CannonTower.INITIAL_DAMAGE
									* Math.pow(CannonTower.DAMAGE_COEFF, tt.getLevel())
									- CannonTower.INITIAL_DAMAGE
											* Math.pow(CannonTower.DAMAGE_COEFF, tt.getLevel() - 1);
						}
					} else if (p0 != null) {
						world.createCannonTower(1, p0.getX(), p0.getY());
						++path_tower_c[pi];
						toweri_c[pi].add(p0);
						towers_path.put(allPoint[p0.getX()][p0.getY()], pi);
						spendedMoney += CannonTower.INITIAL_PRICE + CannonTower.INITIAL_PRICE_INCREASE * all_ct;
						++all_ct;
						doNearBuild(p0);
						path_power_c[pi] += CannonTower.INITIAL_DAMAGE;
					}

				} else if (c2 > c1
						&& money - spendedMoney >= ArcherTower.INITIAL_PRICE
								+ ArcherTower.INITIAL_PRICE_INCREASE * all_at
						&& (my_player.getStormsLeft() == 0 || turn >= 10)
				/* && (turn > min_length_path || turn == 6) */) {
					// no more room ?! and strong weak unit's
					// last tower?
					Point p0;
					if (turn < 100)
						p0 = nlextTower(p, pi);
					else
						p0 = nextTower(p, pi);
					if (path_tower_a[pi] >= 1 && (p0 == null || lastBest <= 2 || all_at > 16)) {
						System.out.println("UPA" + p0.getX() + " - " + p0.getY());
						int tr = (rnd.nextInt() & Integer.MAX_VALUE) % toweri_a[pi].size();
						Tower tt = ((GrassCell) de_map.getCell(toweri_a[pi].get(tr).getX(),
								toweri_a[pi].get(tr).getY())).getTower();
						if (money - spendedMoney >= ArcherTower.INITIAL_LEVEL_UP_PRICE
								* Math.pow(ArcherTower.PRICE_COEFF, tt.getLevel() - 1)) {
							world.upgradeTower(tt);
							spendedMoney += ArcherTower.INITIAL_LEVEL_UP_PRICE
									* Math.pow(ArcherTower.PRICE_COEFF, tt.getLevel() - 1);
							path_power_a[pi] += ArcherTower.INITIAL_DAMAGE
									* Math.pow(ArcherTower.DAMAGE_COEFF, tt.getLevel())
									- ArcherTower.INITIAL_DAMAGE
											* Math.pow(ArcherTower.DAMAGE_COEFF, tt.getLevel() - 1);
						}
					} else if (p0 != null) {
						world.createArcherTower(1, p0.getX(), p0.getY());
						++path_tower_a[pi];
						toweri_a[pi].add(p0);
						towers_path.put(allPoint[p0.getX()][p0.getY()], pi);
						spendedMoney += ArcherTower.INITIAL_PRICE + ArcherTower.INITIAL_PRICE_INCREASE * all_at;
						++all_at;
						doNearBuild(p0);
						path_power_a[pi] += ArcherTower.INITIAL_DAMAGE;
					}
				}
			}
		}
	}

	private void doNearBuild(Point p0) {
		int x = p0.getX();
		int y = p0.getY();
		notNearBuild[x][y] = true;
		for (int i = 0; i < 4; ++i) {
			int xx = x + four_dir_x[i];
			int yy = y + four_dir_y[i];
			if (xx >= 0 && xx < width && yy >= 0 && yy < height) {
				notNearBuild[xx][yy] = true;
			}
		}

	}

	private void bombThem() {
		// just hit them all in some turn search for must explosive bomb
		if (turn % Math.max(24 / path_num, 1) == 3 % Math.max(24 / path_num, 1) && my_player.getStormsLeft() != 0) {
			int best = -1;
			int bpx = -1;
			int bpy = -1;
			for (Path p : de_paths) {
				for (RoadCell rc : p.getRoad()) {
					Point pp = rc.getLocation();
					int x = pp.getX();
					int y = pp.getY();
					int bb = ((RoadCell) de_map.getCell(x, y)).getUnits().size();
					for (int i = 0; i < 12; ++i) {
						int xx = x + tower_range_array_x[i];
						int yy = y + tower_range_array_y[i];
						if (xx >= 0 && xx < width && yy >= 0 && yy < height
								&& de_map.getCell(xx, yy).getClass() == RoadCell.class) {
							bb += ((RoadCell) de_map.getCell(xx, yy)).getUnits().size();
						}
					}
					if (bb >= best) {
						best = bb;
						bpx = x;
						bpy = y;
					}
				}
			}
			if (best != -1 && my_player.getStormsLeft() != 0 && best > 6) {
				world.createStorm(bpx, bpy);
			}
		}
	}

	public int lastBest;

	private Point nextTower(Path p, int pi) {
		for (int i = 0; i < width; ++i)
			for (int j = 0; j < height; ++j)
				good_for_tower[pi][i][j] = 0;
		int best = -1;
		int bpx = -1;
		int bpy = -1;
		for (RoadCell rc : p.getRoad()) {
			Point pp = rc.getLocation();
			int x = pp.getX();
			int y = pp.getY();
			for (int i = 0; i < 12; ++i) {
				int xx = x + tower_range_array_x[i];
				int yy = y + tower_range_array_y[i];
				if (xx >= 0 && xx < width && yy >= 0 && yy < height
						&& de_map.getCell(xx, yy).getClass() == GrassCell.class
						&& ((GrassCell) de_map.getCell(xx, yy)).getTower() == null
						&& world.isTowerConstructable(de_map.getCell(xx, yy)) && !deadCell[xx][yy]
						&& !notNearBuild[xx][yy]) {
					++good_for_tower[pi][xx][yy];
					if (good_for_tower[pi][xx][yy] > best) {
						best = good_for_tower[pi][xx][yy];
						bpx = xx;
						bpy = yy;
					}
				}
			}
		}
		lastBest = best;
		if (best == -1)
			return null;
		System.out.println(((GrassCell) de_map.getCell(bpx, bpy)).getTower() + " XXX "
				+ world.isTowerConstructable(world.getDefenceMap().getCell(bpx, bpy)) + " XXX " + money + " XXX " + bpx
				+ " XXX " + bpy);
		return allPoint[bpx][bpy];
	}

	private Point nlextTower(Path p, int pi) {
		for (int i = 0; i < width; ++i)
			for (int j = 0; j < height; ++j)
				good_for_tower[pi][i][j] = 0;
		int best = -1;
		int bpx = -1;
		int bpy = -1;
		for (RoadCell rc : p.getRoad()) {
			Point pp = rc.getLocation();
			int x = pp.getX();
			int y = pp.getY();
			for (int i = 0; i < 12; ++i) {
				int xx = x + tower_range_array_x[i];
				int yy = y + tower_range_array_y[i];
				if (xx >= 0 && xx < width && yy >= 0 && yy < height
						&& de_map.getCell(xx, yy).getClass() == GrassCell.class
						&& ((GrassCell) de_map.getCell(xx, yy)).getTower() == null
						&& world.isTowerConstructable(de_map.getCell(xx, yy)) && !deadCell[xx][yy]
						&& !notNearBuild[xx][yy]) {
					++good_for_tower[pi][xx][yy];
					if (good_for_tower[pi][xx][yy] >= best) {
						best = good_for_tower[pi][xx][yy];
						bpx = xx;
						bpy = yy;
					}
				}
			}
		}
		lastBest = best;
		if (best == -1)
			return null;
		System.out.println(((GrassCell) de_map.getCell(bpx, bpy)).getTower() + " XXX "
				+ world.isTowerConstructable(world.getDefenceMap().getCell(bpx, bpy)) + " XXX " + money + " XXX " + bpx
				+ " XXX " + bpy);
		return allPoint[bpx][bpy];
	}

}
