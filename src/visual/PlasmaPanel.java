package visual;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import objects.DecimalPoint;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

public class PlasmaPanel extends JPanel {

	private double[][] values;
	private Point[] targets;
	private double rough;
	private double minval;
	private double maxval;
	private double maxHeight = 6;
	private Random rnd = new Random();
	private int squareResolution = 50;
	
	private boolean painting = false;
	private int detail = 3;
	
	private int currentColorScheme = 0;
	static final int REDtoGREEN = 0, BLACKtoWHITE = 1, BLUEtoWHITE = 2;
	
	private double ColorModifier;
	
	public PlasmaPanel(){
		this.setBounds(0, 0, 100, 100);
		this.setBackground(Color.BLACK);
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent arg0) {
				try {
					setSize(values.length*squareResolution/detail, values[0].length*squareResolution/detail);
				}
				catch (Exception e){
					e.printStackTrace();
					setSize(100, 100);
				}
			}
		});
	}
	
	//Generates the height map using a plasma fractal
	public void genorateLandscape(int size, double roughFactor){
		double rough = size * roughFactor;
		this.rough = rough;
		size += detail;
		double seed = System.currentTimeMillis() / 10000.0;
		while (seed > 30){
			seed = seed / 10.0;
		}
		double[][] values = new double[2][2];
		values[0][0] = Math.abs(seed + random());
		values[0][1] = Math.abs(seed + random());
		values[1][0] = Math.abs(seed + random());
		values[1][1] = Math.abs(seed + random());		
		int master = 0;
		while (master <= size){
			values = expand(values);
			int x = 0;
			while (x < values.length){
				int y = 0;
				while (y < values.length){
					if ((x+1) % 2 == 0){
						if ((y+1) % 2 == 0){
							values[x][y] = center(values[x-1][y-1], values[x-1][y+1], values[x+1][y-1], values[x+1][y+1], rough);
						}
						else {
							values[x][y] = midpoint(values[x-1][y], values[x+1][y], rough);
						}
					}
					else {
						if ((y+1) % 2 == 0){
							values[x][y] = midpoint(values[x][y-1], values[x][y+1], rough);
						}
					}
					y++;
				}
				x++;
			}
			rough -= roughFactor;
			if (rough < 0){
				rough = 0;
			}
			master++;
		}
		double[][] values2 = new double[values.length-4-((size+detail)%2+1)][values.length-4-((size+detail)%2+1)];
		int count = 9;
		int x = 0;
		while (x < values.length){
			int y = 0;
			while (y < values.length){
				if (x >= 2 && y >= 2 && x-2 < values2.length && y-2 < values2.length) {
					if (count % 4 == 0){
						values2[x-2][y-2] = (values[x][y] + (values[x-1][y-2] + values[x+1][y+2]) / 2) / 2;
					}
					else if (count % 4 == 1){
						values2[x-2][y-2] = (values[x][y] + (values[x-2][y-1] + values[x+2][y+1]) / 2) / 2;
					}
					else if (count % 4 == 2){
						values2[x-2][y-2] = (values[x][y] + (values[x-2][y+1] + values[x+2][y-1]) / 2) / 2;
					}
					else {
						values2[x-2][y-2] = (values[x][y] + (values[x-1][y+2] + values[x+1][y-2]) / 2) / 2;
					}
				}
				count++;
				if (count == Integer.MAX_VALUE){
					count = 0;
				}
				y++;
			}
			x++;
		}
		this.values = values2;
		minval = getMin();
		maxval = maxHeight;
		x = 0;
		while (x < this.values.length){
			int y = 0;
			while (y < this.values[0].length){
				this.values[x][y] -= minval;
				y++;
			}
			x++;
		}
		minval = 0;
		setColorMultipliers();
		this.setSize(values.length*squareResolution/detail, values[0].length*squareResolution/detail);
	}
	
	//force a height map into the display
	public void setValues(double[][] vals){
		values = vals;
		minval = getMin();
		maxval = maxHeight;
		int x = 0;
		while (x < values.length){
			int y = 0;
			while (y < values[0].length){
				values[x][y] -= minval;
				y++;
			}
			x++;
		}
		minval = 0;
		setColorMultipliers();
		this.setSize(values.length*squareResolution/detail, values[0].length*squareResolution/detail);
		this.repaint();
	}
	
	//paint
	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		painting = true;
		try { 
			int xstart = -(this.getLocation().x / squareResolution * detail) - detail;
			if (xstart < 0){
				xstart = 0;
			}
			int xend = xstart + (this.getParent().getWidth() / squareResolution * detail) + detail*3;
			if (xend > values.length){
				xend = values.length;
			}
			int ystart = -(this.getLocation().y / squareResolution * detail) - detail;
			if (ystart < 0){
				ystart = 0;
			}
			int yend = ystart + (this.getParent().getHeight() / squareResolution * detail) + detail*3;
			if (yend > values[0].length){
				yend = values[0].length;
			}
			if (values.length > 0){
				this.setSize(values.length*squareResolution/detail, values[0].length*squareResolution/detail);
				int x = xstart;
				while (x < xend){
					int y = ystart;
					while (y < yend){
						try {
							int z = 0;
							while (z < targets.length){
								if (x <= (int)targets[z].getX() && (int)targets[z].getX() < x + detail && y <= (int)targets[z].getY() && (int)targets[z].getY() < y+detail){
									g.setColor(Color.MAGENTA);
									break;
								}
								z++;
							}
							if (z == targets.length){
								z = 1/0; // Force Catch Statement
							}
						}
						catch (Exception e){
							try {
								g.setColor(getColor(values[x+detail/2][y+detail/2]));
							} catch (Exception i){
								i.printStackTrace();
								g.setColor(getColor(values[x][y]));
							}
						}
						finally {
							if (x == values.length/2 && y == values.length/2){
								g.setColor(Color.CYAN);
							}
						}
						g.fillRect(x * squareResolution / detail, y * squareResolution / detail, squareResolution, squareResolution);
						switch (currentColorScheme){
						case REDtoGREEN:
						case BLUEtoWHITE:
							g.setColor(Color.DARK_GRAY);
							break;
						case BLACKtoWHITE:
							g.setColor(new Color(240, 250, 0));
							break;
						}
						g.drawRect(x * squareResolution / detail, y * squareResolution / detail, squareResolution, squareResolution);
						y += detail;
					}
					x += detail;
				}
			}
			/*if (trailVis){
				int i = 0;
				while (i < trailPoints.length){
					g.setColor(new Color(255-i*255/trailPoints.length, 255-i*255/trailPoints.length, 255-i*255/trailPoints.length));
					g.fillRect((int)(trailPoints[i].getX()*squareResolution+this.getWidth()/2)-3, (int)(this.getHeight()/2-trailPoints[i].getY()*squareResolution)-3, 6, 6);
					i++;
				}
			}*/
		} catch (Exception e) {
			e.printStackTrace();
		}
		painting = false;
	}

	//force a target distribution
	public void setTargets(Point[] targs){
		targets = targs;
		this.repaint();
	}
	
	//get the target distribution
	public Point[] getTargets(){
		return targets;
	}
	
	//Generate a target distribution
	public void genorateTargets(){
		Point[] targets = new Point[(int)(values.length*values[0].length/(detail*detail)/500.0*(1+rnd.nextInt(5)))];
		int x = 0;
		while (x < targets.length){
			targets[x] = new Point(rnd.nextInt(values.length/detail)*detail+detail/2, rnd.nextInt(values.length/detail)*detail+detail/2);
			x++;
		}
		this.targets = targets;
	}
	
	//is the given point a target
	public boolean targetAtPoint(DecimalPoint loc){
		int x = (int) getMapSquare(loc).getX();
		int y = (int) getMapSquare(loc).getY();
		int i = 0;
		while (i < targets.length){
			if (Math.abs(targets[i].getX() - x) <= detail/2 && Math.abs(targets[i].getY() - y) <= detail/2){
				return true;
			}
			i++;
		}
		return false;
	}
	
	//part of the plasma fractal generation, pushes the array from |x|x|x| to |x|_|x|_|x|
	private double[][] expand(double[][] in){
		double[][] out = new double[in.length * 2 - 1][in.length * 2 - 1];
		int x = 0;
		while (x < in.length){
			int y = 0;
			while (y < in.length){
				out[x*2][y*2] = in[x][y];
				y++;
			}
			x++;
		}
		return out;
	}
	
	private double center(double a, double b, double c, double d, double rough){
		return ((a+b+c+d)/4 + (rough*random()));
	}
	
	private double midpoint(double a, double b, double rough){
		return ((a+b)/2 + (rough*random()));
	}
	
	private double random(){
		int rough = (int)(this.rough * 10.0);
		while (rough < 1){
			rough *= 10;
		}
		double out = rnd.nextInt(rough) + rnd.nextDouble();
		if (rnd.nextBoolean()){
			out *= -1;
		}
		return out;
	}
	
	private double getMax(){
		double max = 0;
		int x = 0;
		while (x < values.length){
			int y = 0;
			while (y < values.length){
				if (values[x][y] > max){
					max = values[x][y];
				}
				y++;
			}
			x++;
		}
		return max;
	}
	
	private double getMin(){
		double min = Integer.MAX_VALUE;
		int x = 0;
		while (x < values.length){
			int y = 0;
			while (y < values.length){
				if (values[x][y] < min){
					min = values[x][y];
				}
				y++;
			}
			x++;
		}
		return min;
	}
	
	private void setColorMultipliers(){
		double x = ((maxval-minval)/2.0+minval);
		ColorModifier = 255 / (x*x - 2*minval*x + minval*minval);
	}
	
	private Color getColor(double numb) {
		switch (currentColorScheme){
		case REDtoGREEN:
			int red = (int)(255 - (ColorModifier*4/3.0)*Math.pow((numb-((maxval-minval)/2.0+minval)), 2));
			if (red < 0){
				red = 0;
			}
			int green = (int)(255 - (ColorModifier*3/4.0)*Math.pow((numb-maxval-0.5), 2));
			if (green < 0){
				green = 0;
			}
			int blue = (int) ((green - 240) / 2 + (numb - maxHeight) * 2);
			if (blue < 0){
				blue = 0;
			}
			if (numb > maxHeight){
				//System.out.println(Math.round(numb*100)/100.0 + ": " + red + ", " + green + ", " + blue);
			}
			return new Color(red, green, blue);
		case BLACKtoWHITE:
			int x = (int) Math.round((numb - minval) / maxval * 255);
			return new Color(x, x, x);
		case BLUEtoWHITE:
			int y = (int) Math.round((numb - minval) / maxval * 255);
			return new Color(y, y, 255);
		default:
			return null;
		}
	}
	
	public void setColorScheme(int which){
		currentColorScheme = which;
		this.repaint();
	}
	
	public void setResolution(int res){
		if (res > 0){
			squareResolution = res;
			this.setSize(values.length*squareResolution/detail, values[0].length*squareResolution/detail);
			this.repaint();
		}
	}
	
	public int getResolution(){
		return squareResolution;
	}
	
	public int getDetail(){
		return detail;
	}
	
	public void SaveImage(double[][] values, int detail, int scheme, String name){
		BufferedImage image = new BufferedImage(15*values.length/detail, 15*values.length/detail, BufferedImage.TYPE_INT_RGB);
		Graphics g = image.getGraphics();   
		double maxval = Double.MIN_VALUE;
		double minval = Double.MAX_VALUE;
		int x = 0;
		while (x < values.length){
			int y = 0;
			while (y < values.length){
				if (values[x][y] > maxval){
					maxval = values[x][y];
				}
				if (values[x][y] < minval){
					minval = values[x][y];
				}
				y++;
			}
			x++;
		}
		x = 0;
		while (x < values.length){
			int y = 0;
			while (y < values.length){
				try {
					switch (scheme){
					case REDtoGREEN:
						int red = (int)(255 - (ColorModifier*4/3.0)*Math.pow((values[x+detail/2][y+detail/2]-((maxval-minval)/2.0+minval)), 2));
						if (red < 0){
							red = 0;
						}
						int green = (int)(255 - (ColorModifier*3/4.0)*Math.pow((values[x+detail/2][y+detail/2]-maxval-0.5), 2));
						if (green < 0){
							green = 0;
						}
						int blue = (int) ((green - 240) / 2 + (values[x+detail/2][y+detail/2] - maxHeight) * 2);
						if (blue < 0){
							blue = 0;
						}
						if (values[x+detail/2][y+detail/2] > maxHeight){
							//System.out.println(Math.round(numb*100)/100.0 + ": " + red + ", " + green + ", " + blue);
						}
						g.setColor(new Color(red, green, blue));
						break;
					case BLACKtoWHITE:
						int i = (int) Math.round((values[x+detail/2][y+detail/2] - minval) / (maxval-minval) * 255);
						g.setColor(new Color(i, i, i));
						break;
					case BLUEtoWHITE:
						int j = (int) Math.round((values[x+detail/2][y+detail/2] - minval) / (maxval-minval) * 255);
						g.setColor(new Color(255-j, 255-j, 255));
						break;
					}
				} catch (Exception i){
					g.setColor(getColor(values[x][y]));
				}
				g.fillRect(x * 15 / detail, y * 15 / detail, 15, 15);
				y += detail;
			}
			x += detail;
		}
		//g.drawString("Hello World!!!", 10, 20);
		try {    
			File output = new File("c:\\Users\\Zac\\Desktop\\" + name + ".png");
			output.createNewFile();
			ImageIO.write(image, "png", output);
			//System.out.println("Save Done");
		} 
		catch (IOException e) {
			e.printStackTrace();   
		}
	}
	
	
	private Point getMapSquare(DecimalPoint loc){ // says which display square a given coordinate falls in
		int shift = values.length / (detail * 2);
		double x = loc.getX() + shift;
		double y = shift - loc.getY();
		int outx = (int)(x*detail);
		int outy = (int)(y*detail);
		return new Point(outx, outy);
	}
}
