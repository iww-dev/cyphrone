package fun.aegis.utils.features.aura.rotations.impl;

import fun.aegis.utils.features.aura.rotations.constructor.RotateConstructor;
import fun.aegis.utils.features.aura.utils.MathAngle;
import fun.aegis.utils.features.aura.warp.Turns;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.security.SecureRandom;

/**
 * HvH V2 - Advanced rotation algorithm powered by Cyphrone Engin
 * Cyphrone Engine: High-performance combat rotation system
 */
public class HAngleV2 extends RotateConstructor {
     private final SecureRandom secureRandom = new SecureRandom();
     
     // Математические константы
     private final double PHI = 1.618033988749895;           // Золотое сечение
     private final double PI = Math.PI;                      // Число Пи
     private final double E = Math.E;                        // Число Эйлера
     private final double SQRT2 = Math.sqrt(2);              // Корень из 2
     private final double SQRT5 = Math.sqrt(5);              // Корень из 5
     
     // Последовательность Фибоначчи
     private final int[] fibonacci = {1, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89, 144};
     
     // Параметры Лоренца для хаотичного движения
     private double lorenzX = 0.1, lorenzY = 0.1, lorenzZ = 0.1;
     private double lyapunovState = 0.5;
     
     // Шум Перлина
     private float[] perlinNoise = new float[512];
     private float noiseOffset = 0;
     
     // История для теории Хауса
     private float lastYawDelta = 0;
     private float lastPitchDelta = 0;
     private int houseCounter = 0;
     
     // === CYPHRONE ENGINE: ADVANCED MATHEMATICS ===
     // Параметры для дополнительной математики
     private double mandelbrotIteration = 0;
     private double juliaSetReal = -0.7;
     private double juliaSetImag = 0.27015;
     private float accelerationFactor = 1.0f;
     private long lastAccelerationUpdate = 0;
     
     // Переменные для дополнительных формул
     private float exponentialSmoothing = 0.5f;
     private float previousYaw = 0;
     private float previousPitch = 0;
     private float hyperbolicsState = 0;
     private float wavePhase = 0;
     
     // Переменные для новых формул
     private float sigmoidState = 0;
     private float reluState = 0;
     private long linearCongruentialState = 12345;
     private float[] movingAverageBuffer = new float[5];
     private int bufferIndex = 0;
     
     // Power Scan система
     private PowerScan powerScan = new PowerScan();

     public HAngleV2() {
          super("HvH V2");
          initPerlinNoise();
     }

     private void initPerlinNoise() {
          for (int i = 0; i < 256; i++) {
               perlinNoise[i] = perlinNoise[256 + i] = secureRandom.nextFloat();
          }
     }

     @Override
     public Turns limitAngleChange(Turns currentAngle, Turns targetAngle, Vec3d vec3d, Entity entity) {
          // === CYPHRONE ENGINE: ROTATION CORE ===
          // Advanced targeting system with mathematical precision
          if (entity instanceof net.minecraft.entity.LivingEntity target) {
               // === CYPHRONE ENGINE: POWER SCAN SYSTEM ===
               // Используем Power Scan для максимального range
               Vec3d targetPos = powerScan.scanBestHitpoint(target);
               targetAngle = MathAngle.calculateAngle(targetPos);
          }

          if (currentAngle == null || targetAngle == null) {
               return currentAngle != null ? currentAngle : new Turns(0, 0);
          }

          Turns angleDelta = MathAngle.calculateDelta(currentAngle, targetAngle);
          float yawDelta = angleDelta.getYaw();
          float pitchDelta = angleDelta.getPitch();

          // === CYPHRONE ENGINE: GOLDEN RATIO MODULE ===
          // Динамическая скорость вращения на основе Золотого Сечения
          double seed = (System.currentTimeMillis() % 2000) / 2000.0;
          float chaoticSpeedYaw = (float) ((seed * PHI) % 1.0 * 30.0);
          float chaoticSpeedPitch = (float) ((seed * (PHI * PHI)) % 1.0 * 20.0);

          // === CYPHRONE ENGINE: FIBONACCI SEQUENCE ADAPTER ===
          // Адаптивная скорость на основе последовательности Фибоначчи
          int fibIndex = (int) (Math.abs(yawDelta) * 10) % fibonacci.length;
          float fibInfluence = fibonacci[fibIndex] / 144.0f;

          float speedYaw = 35.0f + chaoticSpeedYaw + (fibInfluence * 5.0f);
          float speedPitch = 20.0f + chaoticSpeedPitch + (fibInfluence * 3.0f);

          float moveYaw = MathHelper.clamp(yawDelta, -speedYaw, speedYaw);
          float movePitch = MathHelper.clamp(pitchDelta, -speedPitch, speedPitch);

          // === CYPHRONE ENGINE: ACCELERATION DAMPING ===
          // Случайное замедление на 2-9% для более реалистичного движения
          long currentTime = System.currentTimeMillis();
          if (currentTime - lastAccelerationUpdate > 50) {
               accelerationFactor = 0.91f + (secureRandom.nextFloat() * 0.08f); // 0.91 - 0.99
               lastAccelerationUpdate = currentTime;
          }
          moveYaw *= accelerationFactor;
          movePitch *= accelerationFactor;

          float finalYaw = currentAngle.getYaw() + moveYaw;
          float finalPitch = MathHelper.clamp(currentAngle.getPitch() + movePitch, -90F, 90F);

          // === CYPHRONE ENGINE: PERLIN NOISE GENERATOR ===
          // Плавное хаотичное движение через шум Перлина
          float perlinValue = getPerlinNoise(noiseOffset += 0.05f * PHI);
          float perlinJitterYaw = perlinValue * 0.2f;
          float perlinJitterPitch = (float) Math.sin(noiseOffset * PI) * 0.15f;

          // === CYPHRONE ENGINE: LORENZ ATTRACTOR SYSTEM ===
          // Хаотичная система для непредсказуемого поведения
          updateLorenzAttractor();
          float lorenzInfluence = (float) (lorenzX * 0.12f);

          // === CYPHRONE ENGINE: LYAPUNOV CHAOS MEASURE ===
          // Показатель Ляпунова для измерения хаотичности
          float lyapunovNoise = getLyapunovNoise();

          // === CYPHRONE ENGINE: HOUSE THEORY ANALYZER ===
          // Анализ паттернов движения через теорию Хауса
          float housePattern = applyHouseTheory(yawDelta, pitchDelta);

          // === CYPHRONE ENGINE: PI CYCLE MODULE ===
          // Циклическое движение на основе числа Пи
          double piCycle = Math.sin((System.currentTimeMillis() % 6283) / 1000.0) * 0.1;

          // === CYPHRONE ENGINE: APERIODIC JITTER SYSTEM ===
          // Апериодический джиттер для реалистичного движения
          float rotationDifference = (float) Math.hypot(yawDelta, pitchDelta);
          if (rotationDifference > 0.4f) {
               double time = (System.currentTimeMillis() % 10000) / 1000.0;
               double jitterX = Math.sin(time * PHI) * 0.15;
               double jitterY = Math.cos(time * (PHI * PHI)) * 0.15;

               finalYaw += (float) jitterX + perlinJitterYaw + lorenzInfluence + lyapunovNoise + housePattern + (float) piCycle;
               finalPitch = MathHelper.clamp(finalPitch + (float) jitterY + perlinJitterPitch, -90F, 90F);
          }

          // === CYPHRONE ENGINE: ADVANCED MATHEMATICAL SYSTEMS ===
          // Активные математические системы:
          // - Множество Мандельброта: фрактальная геометрия для анализа сложности движения
          // - Множество Жюлиа: параметрическое исследование динамических систем
          // - Геометрия Римана: неевклидова геометрия для криволинейных траекторий
          // - Фазовое пространство: многомерный анализ состояния ротации
          // - Синергетика: самоорганизация в сложных системах
          // - Топология: свойства пространства, инвариантные при непрерывных деформациях
          
          // === CYPHRONE ENGINE: SUBTLE MATHEMATICAL INFLUENCE ===
          // Очень осторожное применение дополнительной математики (влияние < 0.5%)
          if (houseCounter % 100 == 0) {
               // Редко вычисляем параметр порядка синергетики
               float orderParam = calculateOrderParameter(yawDelta, pitchDelta);
               finalYaw += orderParam * 0.001f; // Минимальное влияние
          }

          // === CYPHRONE ENGINE: MANDELBROT SET INFLUENCE ===
          // Фрактальная геометрия для анализа сложности движения (очень малое влияние)
          if (houseCounter % 50 == 0) {
               double mandelbrotValue = calculateMandelbrot(yawDelta / 45.0, pitchDelta / 45.0, 8);
               float mandelbrotInfluence = (float) (mandelbrotValue / 8.0) * 0.0015f; // Очень малое влияние
               finalYaw += mandelbrotInfluence;
          }

          // === CYPHRONE ENGINE: JULIA SET INFLUENCE ===
          // Параметрическое исследование динамических систем
          if (houseCounter % 30 == 0) {
               double juliaValue = calculateJuliaSet(yawDelta / 45.0, pitchDelta / 45.0, 12);
               float juliaInfluence = (float) (juliaValue / 12.0) * 0.008f;
               finalPitch += juliaInfluence;
          }

          // === CYPHRONE ENGINE: RIEMANNIAN GEOMETRY INFLUENCE ===
          // Неевклидова геометрия для криволинейных траекторий (чуть уменьшено)
          if (houseCounter % 40 == 0) {
               double riemannCurvature = calculateRiemannianCurvature(yawDelta, pitchDelta);
               float riemannInfluence = (float) (1.0 / (riemannCurvature + 1.0)) * 0.004f; // Уменьшено
               finalYaw += riemannInfluence;
          }

          // === CYPHRONE ENGINE: PHASE SPACE INFLUENCE ===
          // Многомерный анализ состояния ротации
          if (houseCounter % 35 == 0) {
               double phaseDistance = calculatePhaseSpaceDistance(yawDelta, pitchDelta, lastYawDelta, lastPitchDelta);
               float phaseInfluence = (float) Math.tanh(phaseDistance / 100.0) * 0.006f;
               finalPitch += phaseInfluence;
          }

          // === CYPHRONE ENGINE: TRIGONOMETRIC FORMULAS (1) ===
          // Синусоидальная интерполяция для плавного движения (очень мало)
          if (houseCounter % 60 == 0) {
               float smoothInterp = (float) Math.sin(Math.PI * (houseCounter % 100) / 100.0) * 0.0008f;
               finalYaw += smoothInterp;
          }

          // === CYPHRONE ENGINE: HYPERBOLIC FUNCTIONS (2) ===
          // Гиперболические функции для нелинейного масштабирования (очень мало)
          if (houseCounter % 45 == 0) {
               hyperbolicsState = (float) Math.sinh(yawDelta / 90.0) * 0.0012f;
               finalYaw += hyperbolicsState;
          }

          // === CYPHRONE ENGINE: LOGARITHMIC FUNCTIONS (3) ===
          // Логарифмическое масштабирование (чуть чуть)
          if (houseCounter % 50 == 0) {
               float logScale = (float) Math.log(Math.abs(yawDelta) + 1.0) * 0.002f;
               finalYaw += logScale;
          }

          // === CYPHRONE ENGINE: POWER FUNCTIONS (4) ===
          // Кубический корень для мягкого масштабирования (чуть чуть)
          if (houseCounter % 55 == 0) {
               float cubicRoot = (float) Math.cbrt(pitchDelta / 45.0) * 0.0018f;
               finalPitch += cubicRoot;
          }

          // === CYPHRONE ENGINE: WAVE FUNCTIONS (6) ===
          // Синусоидальная волна для циклического движения (чуть чуть)
          wavePhase += 0.02f;
          if (houseCounter % 40 == 0) {
               float sineWave = (float) Math.sin(wavePhase) * 0.0015f;
               finalYaw += sineWave;
          }

          // === CYPHRONE ENGINE: FRACTAL FUNCTIONS (7) ===
          // Фрактальные функции для самоподобия (чуть чуть)
          if (houseCounter % 65 == 0) {
               float fractalInfluence = calculateFractalInfluence(yawDelta, pitchDelta) * 0.0014f;
               finalPitch += fractalInfluence;
          }

          // === CYPHRONE ENGINE: GEOMETRIC FORMULAS (8) ===
          // Геометрические формулы для анализа углов (очень мало)
          if (houseCounter % 70 == 0) {
               double vectorAngle = Math.atan2(pitchDelta, yawDelta);
               float geometricInfluence = (float) Math.sin(vectorAngle) * 0.0009f;
               finalYaw += geometricInfluence;
          }

          // === CYPHRONE ENGINE: MATRIX OPERATIONS (9) ===
          // Матричные операции для трансформаций (мало)
          if (houseCounter % 75 == 0) {
               float matrixDet = yawDelta * pitchDelta; // Определитель 2x2
               float matrixInfluence = (float) Math.tanh(matrixDet / 2000.0) * 0.0016f;
               finalPitch += matrixInfluence;
          }

          // === CYPHRONE ENGINE: PROBABILITY DISTRIBUTIONS (10) ===
          // Нормальное распределение для взвешивания (чуть чуть)
          if (houseCounter % 80 == 0) {
               float normalDist = calculateNormalDistribution(yawDelta, 0, 45.0f) * 0.0013f;
               finalYaw += normalDist;
          }

          // === CYPHRONE ENGINE: DIFFERENTIAL EQUATIONS (13) ===
          // Экспоненциальный рост и затухающие колебания (ОЧЕНЬ мало)
          if (houseCounter % 85 == 0) {
               float exponentialGrowth = (float) Math.exp(-Math.abs(yawDelta) / 100.0) * 0.0005f;
               finalYaw += exponentialGrowth;
          }

          // === CYPHRONE ENGINE: OPTIMIZATION FUNCTIONS (14) ===
          // Сигмоид для плавного перехода (ОЧЕНЬ мало)
          if (houseCounter % 90 == 0) {
               sigmoidState = calculateSigmoid(yawDelta / 45.0f) * 0.0006f;
               finalYaw += sigmoidState;
          }

          // === CYPHRONE ENGINE: CRYPTOGRAPHIC FUNCTIONS (15) ===
          // Линейный конгруэнтный генератор для PRNG (ОЧЕНЬ мало)
          if (houseCounter % 95 == 0) {
               linearCongruentialState = (1103515245L * linearCongruentialState + 12345) & 0x7fffffffL;
               float cryptoInfluence = (linearCongruentialState / 1073741824.0f - 1.0f) * 0.0004f;
               finalPitch += cryptoInfluence;
          }

          // === CYPHRONE ENGINE: SPECIAL CONSTANTS (16) ===
          // Специальные константы для модуляции (ОЧЕНЬ мало)
          if (houseCounter % 100 == 0) {
               float aperyConstant = 1.202f;
               float catalanConstant = 0.915f;
               float constantInfluence = (float) Math.sin(houseCounter * aperyConstant) * catalanConstant * 0.0003f;
               finalYaw += constantInfluence;
          }

          // === CYPHRONE ENGINE: INTERPOLATION FUNCTIONS (17) ===
          // Кубическая интерполяция Catmull-Rom (ОЧЕНЬ мало)
          if (houseCounter % 105 == 0) {
               float hermiteInterp = calculateHermiteInterpolation(yawDelta, pitchDelta) * 0.0005f;
               finalPitch += hermiteInterp;
          }

          // === CYPHRONE ENGINE: NORMALIZATION FUNCTIONS (18) ===
          // Z-score нормализация (ОЧЕНЬ мало)
          if (houseCounter % 110 == 0) {
               float zScore = (yawDelta - 0) / (45.0f + 1e-6f) * 0.0004f;
               finalYaw += zScore;
          }

          // === CYPHRONE ENGINE: DISTANCE METRICS (19) ===
          // Манхэттенское расстояние (ОЧЕНЬ мало)
          if (houseCounter % 115 == 0) {
               float manhattanDist = (Math.abs(yawDelta) + Math.abs(pitchDelta)) / 90.0f * 0.0005f;
               finalPitch += manhattanDist;
          }

          // === CYPHRONE ENGINE: FILTERS AND SMOOTHING (20) ===
          // Экспоненциальное сглаживание (ОЧЕНЬ мало)
          if (houseCounter % 120 == 0) {
               float alpha = 0.3f;
               float smoothed = alpha * yawDelta + (1 - alpha) * previousYaw;
               previousYaw = smoothed;
               finalYaw += (smoothed - yawDelta) * 0.0003f;
          }

          // === CYPHRONE ENGINE: ROTATION VALIDATOR ===
          // Валидация поворота для предотвращения экстремальных скачков
          float maxYawStep = 60.0f;
          float maxPitchStep = 40.0f;
          
          float yawDiff = MathHelper.wrapDegrees(finalYaw - currentAngle.getYaw());
          float pitchDiff = finalPitch - currentAngle.getPitch();
          
          if (Math.abs(yawDiff) > maxYawStep) {
               yawDiff = Math.copySign(maxYawStep, yawDiff);
               finalYaw = currentAngle.getYaw() + yawDiff;
          }
          
          if (Math.abs(pitchDiff) > maxPitchStep) {
               pitchDiff = Math.copySign(maxPitchStep, pitchDiff);
               finalPitch = currentAngle.getPitch() + pitchDiff;
          }
          
          finalPitch = MathHelper.clamp(finalPitch, -90F, 90F);

          Turns result = new Turns(finalYaw, finalPitch);
          return result.adjustSensitivity();
     }

     /**
      * CYPHRONE ENGINE: Perlin Noise Generator
      * Плавное хаотичное движение через шум Перлина
      */
     private float getPerlinNoise(float x) {
          int X = (int) Math.floor(x) & 255;
          x -= Math.floor(x);
          float u = x * x * x * (x * (x * 6 - 15) + 10);
          int nextIdx = (X + 1) & 255;
          return MathHelper.lerp(u, perlinNoise[X], perlinNoise[nextIdx]);
     }

     /**
      * CYPHRONE ENGINE: Lorenz Attractor System
      * Хаотичная система для непредсказуемого поведения
      */
     private void updateLorenzAttractor() {
          double sigma = 10.0;
          double rho = 28.0;
          double beta = 8.0 / 3.0;
          double dt = 0.001;

          double dxdt = sigma * (lorenzY - lorenzX);
          double dydt = lorenzX * (rho - lorenzZ) - lorenzY;
          double dzdt = lorenzX * lorenzY - beta * lorenzZ;

          lorenzX += dxdt * dt;
          lorenzY += dydt * dt;
          lorenzZ += dzdt * dt;

          double magnitude = Math.sqrt(lorenzX * lorenzX + lorenzY * lorenzY + lorenzZ * lorenzZ);
          if (magnitude > 0) {
               lorenzX /= magnitude;
               lorenzY /= magnitude;
               lorenzZ /= magnitude;
          }
     }

     /**
      * CYPHRONE ENGINE: Lyapunov Chaos Measure
      * Показатель Ляпунова для измерения хаотичности
      */
     private float getLyapunovNoise() {
          double r = 3.9 + (System.currentTimeMillis() % 100 / 1000.0);
          lyapunovState = r * lyapunovState * (1 - lyapunovState);
          if (Double.isNaN(lyapunovState) || Double.isInfinite(lyapunovState)) {
               lyapunovState = 0.5;
          }
          return (float) ((lyapunovState - 0.5) * 2.0);
     }

     /**
      * CYPHRONE ENGINE: House Theory Analyzer
      * Анализ паттернов движения через теорию Хауса
      */
     private float applyHouseTheory(float yawDelta, float pitchDelta) {
          houseCounter++;
          
          float yawChange = Math.abs(yawDelta - lastYawDelta);
          float pitchChange = Math.abs(pitchDelta - lastPitchDelta);
          
          lastYawDelta = yawDelta;
          lastPitchDelta = pitchDelta;
          
          if (yawChange < 0.5f && pitchChange < 0.5f) {
               int pattern = fibonacci[houseCounter % fibonacci.length];
               return (float) Math.sin(houseCounter * PI / pattern) * 0.08f;
          }
          
          return 0;
     }

     @Override
     public Vec3d randomValue() {
          return new Vec3d(0, 0, 0);
     }

     /**
      * CYPHRONE ENGINE: Mandelbrot Set Calculator
      * Фрактальная геометрия для анализа сложности движения
      * Формула: z(n+1) = z(n)² + c
      */
     private double calculateMandelbrot(double real, double imag, int maxIterations) {
          double zReal = 0, zImag = 0;
          for (int i = 0; i < maxIterations; i++) {
               double zRealSquared = zReal * zReal;
               double zImagSquared = zImag * zImag;
               if (zRealSquared + zImagSquared > 4.0) return i;
               double temp = zRealSquared - zImagSquared + real;
               zImag = 2.0 * zReal * zImag + imag;
               zReal = temp;
          }
          return maxIterations;
     }

     /**
      * CYPHRONE ENGINE: Julia Set Calculator
      * Параметрическое исследование динамических систем
      * Используется для анализа устойчивости траекторий
      */
     private double calculateJuliaSet(double real, double imag, int maxIterations) {
          double zReal = real, zImag = imag;
          for (int i = 0; i < maxIterations; i++) {
               double zRealSquared = zReal * zReal;
               double zImagSquared = zImag * zImag;
               if (zRealSquared + zImagSquared > 4.0) return i;
               double temp = zRealSquared - zImagSquared + juliaSetReal;
               zImag = 2.0 * zReal * zImag + juliaSetImag;
               zReal = temp;
          }
          return maxIterations;
     }

     /**
      * CYPHRONE ENGINE: Riemannian Geometry Calculator
      * Неевклидова геометрия для криволинейных траекторий
      * Вычисляет кривизну пространства
      */
     private double calculateRiemannianCurvature(float yawDelta, float pitchDelta) {
          // Метрический тензор для сферической геометрии
          double theta = Math.toRadians(yawDelta);
          double phi = Math.toRadians(pitchDelta);
          double sinTheta = Math.sin(theta);
          
          // Гауссова кривизна сферы
          return 1.0 / (sinTheta * sinTheta + 1e-6);
     }

     /**
      * CYPHRONE ENGINE: Phase Space Analyzer
      * Многомерный анализ состояния ротации
      * Вычисляет расстояние в фазовом пространстве
      */
     private double calculatePhaseSpaceDistance(float yawDelta, float pitchDelta, 
                                                float lastYaw, float lastPitch) {
          // Евклидово расстояние в 4D фазовом пространстве
          double dq1 = yawDelta - lastYaw;
          double dq2 = pitchDelta - lastPitch;
          double dp1 = (yawDelta * yawDelta) - (lastYaw * lastYaw);
          double dp2 = (pitchDelta * pitchDelta) - (lastPitch * lastPitch);
          
          return Math.sqrt(dq1*dq1 + dq2*dq2 + dp1*dp1 + dp2*dp2);
     }

     /**
      * CYPHRONE ENGINE: Synergetics Order Parameter
      * Самоорганизация в сложных системах
      * Параметр порядка для анализа синхронизации
      */
     private float calculateOrderParameter(float yawDelta, float pitchDelta) {
          // Параметр порядка: мера согласованности движения
          float magnitude = (float) Math.sqrt(yawDelta * yawDelta + pitchDelta * pitchDelta);
          return (float) Math.tanh(magnitude / 45.0); // Нормализация
     }

     /**
      * CYPHRONE ENGINE: Fractal Influence Calculator
      * Фрактальные функции для самоподобия
      */
     private float calculateFractalInfluence(float yawDelta, float pitchDelta) {
          // Простая фрактальная функция на основе Коха
          float magnitude = (float) Math.sqrt(yawDelta * yawDelta + pitchDelta * pitchDelta);
          return (float) Math.sin(magnitude * 0.1) * (float) Math.cos(magnitude * 0.05);
     }

     /**
      * CYPHRONE ENGINE: Normal Distribution Calculator
      * Нормальное распределение для взвешивания
      */
     private float calculateNormalDistribution(float x, float mean, float stdDev) {
          float exponent = -((x - mean) * (x - mean)) / (2 * stdDev * stdDev);
          return (float) Math.exp(exponent);
     }

     /**
      * CYPHRONE ENGINE: Sigmoid Function
      * Сигмоид для плавного перехода 0-1
      */
     private float calculateSigmoid(float x) {
          return 1.0f / (1.0f + (float) Math.exp(-x));
     }

     /**
      * CYPHRONE ENGINE: Hermite Interpolation
      * Интерполяция Эрмита для гладких переходов
      */
     private float calculateHermiteInterpolation(float yaw, float pitch) {
          float t = (houseCounter % 100) / 100.0f;
          float t2 = t * t;
          float t3 = t2 * t;
          // Базовые функции Эрмита
          float h00 = 2*t3 - 3*t2 + 1;
          float h10 = t3 - 2*t2 + t;
          return h00 * yaw + h10 * pitch;
     }

     /**
      * CYPHRONE ENGINE: Swish Activation
      * Swish: x * sigmoid(x) для гладкой активации
      */
     private float calculateSwish(float x) {
          return x * calculateSigmoid(x);
     }

     /**
      * CYPHRONE ENGINE: Chebyshev Distance
      * Расстояние Чебышева: max(|x_i - y_i|)
      */
     private float calculateChebyshevDistance(float yaw, float pitch) {
          return Math.max(Math.abs(yaw), Math.abs(pitch));
     }

     /**
      * CYPHRONE ENGINE: Moving Average Filter
      * Скользящее среднее для сглаживания
      */
     private float calculateMovingAverage(float value) {
          movingAverageBuffer[bufferIndex] = value;
          bufferIndex = (bufferIndex + 1) % movingAverageBuffer.length;
          float sum = 0;
          for (float v : movingAverageBuffer) {
               sum += v;
          }
          return sum / movingAverageBuffer.length;
     }
}
