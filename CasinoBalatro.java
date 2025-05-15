import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.Random;
import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class CasinoBalatro extends JFrame {
    private final String[] symbolNames = {"7", "bar", "campana", "mascara", "diamante", "cereza"};
    private ImageIcon[] symbols;
    private JLabel[] reels = new JLabel[3];
    private JLabel creditLabel, winLabel, payoutTableLabel, titleLabel;
    private int credits = 9999;
    private boolean isSpinning = false;
    private Timer[] reelTimers = new Timer[3];
    private Random random = new Random();
    private int[] spinCounters = new int[3];
    private int[] stopPositions = {30, 45, 60};
    private int[] finalSymbols = new int[3];
    private ImageIcon leverUpIcon, leverDownIcon, backgroundImage;
    private JButton leverButton;
    private Clip spinSound, winSound, loseSound;
    private JPanel mainPanel, reelsPanel;
    private JProgressBar jackpotMeter;
    private int jackpot = 0;
    private Timer winAnimationTimer;
    private Color goldColor = new Color(212, 175, 55);
    private JButton addCreditsButton;
    private GradientPanel slotMachinePanel;
    
    // Clase para paneles con degradado
    class GradientPanel extends JPanel {
        private Color color1, color2;
        
        public GradientPanel(Color color1, Color color2) {
            this.color1 = color1;
            this.color2 = color2;
            setOpaque(false);
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            int w = getWidth();
            int h = getHeight();
            GradientPaint gp = new GradientPaint(0, 0, color1, 0, h, color2);
            g2d.setPaint(gp);
            g2d.fillRect(0, 0, w, h);
        }
    }
    
    // Clase para botones redondeados con efectos
    class StyledButton extends JButton {
        private Color hoverColor;
        private Color normalColor;
        private Color pressedColor;
        
        public StyledButton(String text, Color normalColor, Color hoverColor, Color pressedColor) {
            super(text);
            this.normalColor = normalColor;
            this.hoverColor = hoverColor;
            this.pressedColor = pressedColor;
            
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setForeground(Color.WHITE);
            setFont(new Font("Arial", Font.BOLD, 14));
            
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    setBackground(StyledButton.this.hoverColor);
                }
                
                @Override
                public void mouseExited(MouseEvent e) {
                    setBackground(StyledButton.this.normalColor);
                }
                
                @Override
                public void mousePressed(MouseEvent e) {
                    setBackground(StyledButton.this.pressedColor);
                }
                
                @Override
                public void mouseReleased(MouseEvent e) {
                    setBackground(StyledButton.this.hoverColor);
                }
            });
            
            setBackground(normalColor);
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            if (getModel().isPressed()) {
                g2.setColor(pressedColor);
            } else if (getModel().isRollover()) {
                g2.setColor(hoverColor);
            } else {
                g2.setColor(normalColor);
            }
            
            g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 15, 15));
            
            // Efecto de brillo
            GradientPaint gp = new GradientPaint(
                0, 0, new Color(255, 255, 255, 100),
                0, getHeight(), new Color(255, 255, 255, 0));
            g2.setPaint(gp);
            g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight() / 2, 15, 15));
            
            g2.dispose();
            
            super.paintComponent(g);
        }
        
        @Override
        protected void paintBorder(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(120, 120, 120));
            g2.draw(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 15, 15));
            g2.dispose();
        }
    }
    
    public CasinoBalatro() {
        setTitle("Tragaperras Balatro");
        setSize(600, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // Hacemos la ventana no redimensionable para mantener la apariencia
        setResizable(false);
        
        loadImages();
        loadSounds();
        setupUI();
        
        setVisible(true);
    }

    private void loadImages() {
        symbols = new ImageIcon[symbolNames.length];
        for (int i = 0; i < symbolNames.length; i++) {
            try {
                symbols[i] = new ImageIcon(getClass().getResource("/imagenes/" + symbolNames[i] + ".png"));
                Image img = symbols[i].getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
                symbols[i] = new ImageIcon(img);
            } catch (Exception e) {
                // Si no se puede cargar la imagen, crear una alternativa
                symbols[i] = createSymbolIcon(symbolNames[i]);
                System.out.println("No se pudo cargar la imagen: " + symbolNames[i] + ".png - Usando alternativa");
            }
        }

        try {
            backgroundImage = new ImageIcon(getClass().getResource("/imagenes/casino_background.png"));
            Image bgImg = backgroundImage.getImage().getScaledInstance(600, 800, Image.SCALE_SMOOTH);
            backgroundImage = new ImageIcon(bgImg);
        } catch (Exception e) {
            backgroundImage = null;
            System.out.println("No se pudo cargar el fondo. Usando colores degradados en su lugar.");
        }
        
        try {
            leverUpIcon = new ImageIcon(getClass().getResource("/imagenes/palanca_up.png"));
            leverDownIcon = new ImageIcon(getClass().getResource("/imagenes/palanca_down.png"));
            
            Image upImg = leverUpIcon.getImage().getScaledInstance(60, 150, Image.SCALE_SMOOTH);
            leverUpIcon = new ImageIcon(upImg);
            
            Image downImg = leverDownIcon.getImage().getScaledInstance(60, 150, Image.SCALE_SMOOTH);
            leverDownIcon = new ImageIcon(downImg);
        } catch (Exception e) {
            // Si falla, crea iconos simples
            leverUpIcon = createSimpleLeverIcon(false);
            leverDownIcon = createSimpleLeverIcon(true);
            System.out.println("No se pudo cargar las imágenes de la palanca. Usando alternativas");
        }
    }
    
    private ImageIcon createSymbolIcon(String symbolName) {
        // Crea un icono simple para los símbolos si no se encuentra la imagen
        int size = 100;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        
        // Configurar para alta calidad
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Fondo del símbolo
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, size, size);
        
        // Borde del símbolo
        g2d.setColor(goldColor);
        g2d.setStroke(new BasicStroke(3));
        g2d.drawRect(3, 3, size - 6, size - 6);
        
        // Dibujar el símbolo según su nombre
        g2d.setFont(new Font("Arial", Font.BOLD, 30));
        g2d.setColor(Color.WHITE);
        
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(symbolName.toUpperCase());
        int textHeight = fm.getHeight();
        
        g2d.drawString(symbolName.toUpperCase(), (size - textWidth) / 2, size / 2 + textHeight / 4);
        
        g2d.dispose();
        return new ImageIcon(image);
    }
    
    private ImageIcon createSimpleLeverIcon(boolean down) {
        // Crea un icono simple para la palanca si no se encuentra la imagen
        int width = 60;
        int height = 150;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        
        // Dibujar la base de la palanca
        g2d.setColor(new Color(120, 120, 120));
        g2d.fillRoundRect(20, height - 30, 20, 30, 5, 5);
        
        // Dibujar la palanca
        g2d.setColor(new Color(180, 0, 0));
        if (down) {
            g2d.fillRoundRect(15, 40, 30, height - 50, 10, 10);
        } else {
            g2d.fillRoundRect(15, 20, 30, height - 50, 10, 10);
        }
        
        // Dibujar la manija
        g2d.setColor(new Color(220, 220, 0));
        if (down) {
            g2d.fillOval(5, 30, 50, 30);
        } else {
            g2d.fillOval(5, 10, 50, 30);
        }
        
        g2d.dispose();
        return new ImageIcon(image);
    }

    private void loadSounds() {
        spinSound = loadClip("/sonidos/spin.wav");
        winSound = loadClip("/sonidos/win.wav");
        loseSound = loadClip("/sonidos/lose.wav");
        
        // Si no se encuentran los sonidos, genera un mensaje pero continúa
        if (spinSound == null) {
            System.out.println("No se pudo cargar el sonido de giro");
        }
        if (winSound == null) {
            System.out.println("No se pudo cargar el sonido de victoria");
        }
        if (loseSound == null) {
            System.out.println("No se pudo cargar el sonido de derrota");
        }
    }

    private Clip loadClip(String path) {
        try {
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(getClass().getResource(path));
            Clip clip = AudioSystem.getClip();
            clip.open(audioIn);
            return clip;
        } catch (Exception e) {
            System.out.println("Error al cargar sonido: " + path);
            return null;
        }
    }

    private void playSound(Clip clip) {
        if (clip != null) {
            if (clip.isRunning()) clip.stop();
            clip.setFramePosition(0);
            clip.start();
        }
    }

    private void setupUI() {
        // Panel principal con imagen de fondo o degradado
        mainPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (backgroundImage != null) {
                    g.drawImage(backgroundImage.getImage(), 0, 0, getWidth(), getHeight(), this);
                } else {
                    // Degradado como fondo alternativo
                    Graphics2D g2d = (Graphics2D) g;
                    GradientPaint gp = new GradientPaint(
                        0, 0, new Color(25, 25, 112),
                        0, getHeight(), new Color(10, 10, 50));
                    g2d.setPaint(gp);
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                }
            }
        };
        setContentPane(mainPanel);
        
        // Panel de título
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setOpaque(false);
        titlePanel.setBorder(new EmptyBorder(15, 15, 10, 15));
        
        titleLabel = new JLabel("CASINO BALATRONICO");
        titleLabel.setFont(new Font("Impact", Font.BOLD, 36));
        titleLabel.setForeground(goldColor);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        // Efecto de borde para el título
        titleLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(2, 2, 2, 2, new Color(128, 0, 0)),
            BorderFactory.createEmptyBorder(5, 15, 5, 15)
        ));
        
        titlePanel.add(titleLabel, BorderLayout.CENTER);
        
        // Medidor de jackpot
        JPanel jackpotPanel = new JPanel(new BorderLayout());
        jackpotPanel.setOpaque(false);
        jackpotPanel.setBorder(new EmptyBorder(0, 30, 10, 30));
        
        JLabel jackpotLabel = new JLabel("JACKPOT");
        jackpotLabel.setFont(new Font("Arial", Font.BOLD, 18));
        jackpotLabel.setForeground(Color.RED);
        jackpotLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        jackpotMeter = new JProgressBar();
        jackpotMeter.setStringPainted(true);
        jackpotMeter.setString("$" + jackpot);
        jackpotMeter.setValue(50); // Valor inicial
        jackpotMeter.setForeground(new Color(255, 215, 0));
        jackpotMeter.setBackground(new Color(50, 50, 50));
        jackpotMeter.setFont(new Font("Arial", Font.BOLD, 16));
        
        jackpotPanel.add(jackpotLabel, BorderLayout.NORTH);
        jackpotPanel.add(jackpotMeter, BorderLayout.CENTER);
        
        // Panel de la máquina tragaperras (con apariencia metálica)
        slotMachinePanel = new GradientPanel(new Color(80, 80, 80), new Color(30, 30, 30));
        slotMachinePanel.setLayout(new BorderLayout());
        slotMachinePanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(10, 20, 20, 20),
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(100, 100, 100), 3),
                BorderFactory.createBevelBorder(1, new Color(120, 120, 120), new Color(40, 40, 40))
            )
        ));
        
        // Panel de los rodillos con fondo oscuro y borde decorativo
        reelsPanel = new JPanel(new GridLayout(1, 3, 10, 0));
        reelsPanel.setBackground(new Color(0, 0, 0));
        reelsPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(goldColor, 3),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        
        // Crear rodillos con aspecto mejorado
        for (int i = 0; i < 3; i++) {
            JPanel reelContainer = new JPanel();
            reelContainer.setLayout(new BorderLayout());
            reelContainer.setBackground(new Color(30, 30, 30));
            reelContainer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(50, 50, 50), 2),
                BorderFactory.createBevelBorder(1, new Color(20, 20, 20), new Color(70, 70, 70))
            ));
            
            reels[i] = new JLabel(symbols[random.nextInt(symbols.length)]);
            reels[i].setHorizontalAlignment(SwingConstants.CENTER);
            reels[i].setVerticalAlignment(SwingConstants.CENTER);
            reels[i].setOpaque(true);
            reels[i].setBackground(new Color(0, 0, 0));
            
            reelContainer.add(reels[i], BorderLayout.CENTER);
            reelsPanel.add(reelContainer);
        }
        
        // Panel de controles
        JPanel controlPanel = new JPanel(new BorderLayout(10, 10));
        controlPanel.setOpaque(false);
        controlPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // Panel de la palanca
        JPanel leverPanel = new JPanel(new BorderLayout());
        leverPanel.setOpaque(false);
        
        leverButton = new JButton(leverUpIcon);
        leverButton.setPreferredSize(new Dimension(80, 180));
        leverButton.setBorderPainted(false);
        leverButton.setContentAreaFilled(false);
        leverButton.setFocusPainted(false);
        
        leverButton.addActionListener(e -> {
            if (!isSpinning && credits > 0) {
                // Animación de la palanca
                leverButton.setIcon(leverDownIcon);
                Timer returnTimer = new Timer(200, ev -> leverButton.setIcon(leverUpIcon));
                returnTimer.setRepeats(false);
                returnTimer.start();
                
                startSpin();
            }
        });
        
        leverPanel.add(leverButton, BorderLayout.CENTER);
        
        // Panel de información y botones
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);
        infoPanel.setBorder(new EmptyBorder(10, 20, 10, 20));
        
        // Etiqueta de créditos con aspecto de display digital
        JPanel creditDisplay = new JPanel();
        creditDisplay.setBackground(new Color(0, 0, 0));
        creditDisplay.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(goldColor, 2),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        
        creditLabel = new JLabel("CRÉDITOS: " + credits);
        creditLabel.setForeground(new Color(255, 50, 50));
        creditLabel.setFont(new Font("Digital-7", Font.BOLD, 24));
        if (!isFontAvailable("Digital-7")) {
            creditLabel.setFont(new Font("Courier New", Font.BOLD, 24));
        }
        creditLabel.setHorizontalAlignment(SwingConstants.CENTER);
        creditDisplay.add(creditLabel);
        
        // Etiqueta de ganancia
        JPanel winDisplay = new JPanel();
        winDisplay.setBackground(new Color(0, 0, 0));
        winDisplay.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(goldColor, 2),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        
        winLabel = new JLabel("¡BUENA SUERTE!");
        winLabel.setForeground(Color.YELLOW);
        winLabel.setFont(new Font("Impact", Font.BOLD, 22));
        winLabel.setHorizontalAlignment(SwingConstants.CENTER);
        winDisplay.add(winLabel);
        
        // Botón para añadir créditos
        addCreditsButton = new StyledButton("AÑADIR CRÉDITOS", 
            new Color(0, 100, 0), 
            new Color(0, 150, 0), 
            new Color(0, 80, 0));
        addCreditsButton.addActionListener(e -> {
            credits += 50;
            creditLabel.setText("CRÉDITOS: " + credits);
            playSound(winSound);
        });
        
        // Tabla de pagos
        JPanel payoutPanel = new GradientPanel(new Color(20, 20, 80), new Color(10, 10, 40));
        payoutPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(goldColor, 2),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        payoutPanel.setLayout(new GridLayout(3, 2, 5, 5));
        
        //tabla de pagos
        String[] payoutInfo = {
            "7 = 500", "BAR = 300",
            "CAMPANA = 150", "MÁSCARA = 200",
            "DIAMANTE = 400", "CEREZA = 100"
        };
        
        for (String info : payoutInfo) {
            JLabel payoutLabel = new JLabel(info);
            payoutLabel.setFont(new Font("Arial", Font.BOLD, 14));
            payoutLabel.setForeground(Color.WHITE);
            payoutLabel.setHorizontalAlignment(SwingConstants.CENTER);
            payoutPanel.add(payoutLabel);
        }
        
        // Organizar los componentes
        infoPanel.add(creditDisplay);
        infoPanel.add(Box.createVerticalStrut(10));
        infoPanel.add(winDisplay);
        infoPanel.add(Box.createVerticalStrut(10));
        infoPanel.add(addCreditsButton);
        infoPanel.add(Box.createVerticalStrut(15));
        infoPanel.add(payoutPanel);
        
        controlPanel.add(leverPanel, BorderLayout.EAST);
        controlPanel.add(infoPanel, BorderLayout.CENTER);
        
        // Añadir todo al panel principal
        slotMachinePanel.add(reelsPanel, BorderLayout.CENTER);
        slotMachinePanel.add(controlPanel, BorderLayout.SOUTH);
        
        mainPanel.add(titlePanel, BorderLayout.NORTH);
        mainPanel.add(jackpotPanel, BorderLayout.SOUTH);
        mainPanel.add(slotMachinePanel, BorderLayout.CENTER);
    }
    
    private boolean isFontAvailable(String fontName) {
        Font[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
        for (Font font : fonts) {
            if (font.getFontName().equals(fontName)) {
                return true;
            }
        }
        return false;
    }

    private void startSpin() {
        if (isSpinning || credits <= 0) return;
        isSpinning = true;
        credits--;
        creditLabel.setText("CRÉDITOS: " + credits);
        winLabel.setText("¡GIRANDO!");
        winLabel.setForeground(Color.WHITE);
        
        // Incrementar jackpot con cada giro
        jackpot += 100;
        jackpotMeter.setString("$" + jackpot);
        
        playSound(spinSound);
        
        for (int i = 0; i < 3; i++) {
            spinCounters[i] = 0;
            finalSymbols[i] = random.nextInt(symbols.length);
            int reelIndex = i;
            
            if (reelTimers[i] != null && reelTimers[i].isRunning()) reelTimers[i].stop();
            
            // Velocidad variable entre los rodillos
            int delay = 50 + (i * 10);
            reelTimers[i] = new Timer(delay, e -> spinReel(reelIndex));
            reelTimers[i].start();
        }
    }

    private void spinReel(int index) {
        spinCounters[index]++;
        int currentSymbol = random.nextInt(symbols.length);
        reels[index].setIcon(symbols[currentSymbol]);
        
        // Efecto de difuminado mientras gira
        reels[index].getParent().setBackground(new Color(20, 20, 20, 150));
        
        if (spinCounters[index] >= stopPositions[index]) {
            reelTimers[index].stop();
            reels[index].setIcon(symbols[finalSymbols[index]]);
            
            // Restaurar el fondo normal cuando se detiene
            reels[index].getParent().setBackground(new Color(30, 30, 30));
            
            // Efecto de parada con flash
            Timer flashTimer = new Timer(80, e -> {
                if (((Timer)e.getSource()).getActionCommand().equals("on")) {
                    reels[index].getParent().setBackground(new Color(80, 80, 80));
                    ((Timer)e.getSource()).setActionCommand("off");
                } else {
                    reels[index].getParent().setBackground(new Color(30, 30, 30));
                    ((Timer)e.getSource()).stop();
                }
            });
            flashTimer.setActionCommand("on");
            flashTimer.setRepeats(true);
            flashTimer.start();
            
            boolean allStopped = true;
            for (Timer timer : reelTimers) {
                if (timer != null && timer.isRunning()) {
                    allStopped = false;
                    break;
                }
            }
            
            if (allStopped) {
                isSpinning = false;
                evaluateResult();
            }
        }
    }

    private void evaluateResult() {
        int s1 = finalSymbols[0];
        int s2 = finalSymbols[1];
        int s3 = finalSymbols[2];
        int payout = 0;

        if (s1 == s2 && s2 == s3) {
            switch (symbolNames[s1]) {
                case "7": payout = 500; break;
                case "bar": payout = 300; break;
                case "campana": payout = 150; break;
                case "mascara": payout = 200; break;
                case "diamante": payout = 400; break;
                case "cereza": payout = 100; break;
            }
            
            credits += payout;
            winLabel.setText("¡GANASTE " + payout + " CRÉDITOS!");
            winLabel.setForeground(Color.YELLOW);
            
            playSound(winSound);
            startWinAnimation();
        } else {
            winLabel.setText("¡SIN PREMIO!");
            winLabel.setForeground(new Color(255, 100, 100));
            
            playSound(loseSound);
        }
        
        creditLabel.setText("CRÉDITOS: " + credits);
        
        // Si se queda sin créditos
        if (credits <= 0) {
            winLabel.setText("¡SIN CRÉDITOS! AÑADIR MÁS");
            winLabel.setForeground(Color.RED);
            
            // Animar el botón de añadir créditos
            Timer blinkTimer = new Timer(300, e -> {
                if (addCreditsButton.getForeground() == Color.WHITE) {
                    addCreditsButton.setForeground(Color.YELLOW);
                } else {
                    addCreditsButton.setForeground(Color.WHITE);
                }
            });
            blinkTimer.setRepeats(true);
            blinkTimer.start();
            
            // Parar después de unos segundos
            Timer stopBlinkTimer = new Timer(3000, e -> {
                blinkTimer.stop();
                addCreditsButton.setForeground(Color.WHITE);
            });
            stopBlinkTimer.setRepeats(false);
            stopBlinkTimer.start();
        }
    }
    
    private void startWinAnimation() {
        // Detener animación anterior si existe
        if (winAnimationTimer != null && winAnimationTimer.isRunning()) {
            winAnimationTimer.stop();
        }
        
        // Crear nueva animación
        final int[] colorIndex = {0};
        final Color[] colors = {
            Color.YELLOW, Color.WHITE, new Color(255, 215, 0), Color.ORANGE
        };
        
        winAnimationTimer = new Timer(150, e -> {
            // Cambiar color de los marcos de los rodillos
            for (int i = 0; i < 3; i++) {
                ((JComponent) reels[i].getParent()).setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(colors[colorIndex[0]], 2),
                    BorderFactory.createBevelBorder(1, colors[colorIndex[0]], new Color(70, 70, 70))
                ));
            }
            
            // Cambiar color del título
            winLabel.setForeground(colors[colorIndex[0]]);
            
            // Avanzar al siguiente color
            colorIndex[0] = (colorIndex[0] + 1) % colors.length;
        });
        
        winAnimationTimer.setRepeats(true);
        winAnimationTimer.start();
        
        // Detener después de unos segundos
        Timer stopTimer = new Timer(3000, e -> {
            winAnimationTimer.stop();
            
            // Restaurar bordes normales
            for (int i = 0; i < 3; i++) {
                ((JComponent) reels[i].getParent()).setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(50, 50, 50), 2),
                    BorderFactory.createBevelBorder(1, new Color(20, 20, 20), new Color(70, 70, 70))
                ));
            }
            
            winLabel.setForeground(Color.YELLOW);
        });
        stopTimer.setRepeats(false);
        stopTimer.start();
    }

    public static void main(String[] args) {
        try {
            // Intentar usar el look and feel del sistema
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(CasinoBalatro::new);
    }
}