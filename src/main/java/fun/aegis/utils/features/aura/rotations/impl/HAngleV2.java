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

          // === CYPHRONE ENGINE: DISTANCE-BASED ACCELERATION ===
          // Ускорение при близкой дистанции (≤1 блока)
          float accelerationBoost = 1.0f;
          float closeRangeSpeedBoost = 1.0f;
          if (entity instanceof net.minecraft.entity.LivingEntity target && mc.player != null) {
               double distance = mc.player.distanceTo(target);
               if (distance <= 1.0) {
                    // Рандомизация ускорения 2-11% при близкой дистанции
                    accelerationBoost = 1.02f + (secureRandom.nextFloat() * 0.09f); // 1.02 - 1.11
                    // Повышение скорости поворота персонажа вблизи на 3-6% (чуть ниже)
                    closeRangeSpeedBoost = 1.03f + (secureRandom.nextFloat() * 0.03f); // 1.03 - 1.06
               }
          }

          // === CYPHRONE ENGINE: SMOOTH BASE SPEEDS ===
          // Менее агрессивная базовая скорость
          float speedYaw = 38.0f * accelerationBoost * closeRangeSpeedBoost;      // Снижено с 45
          float speedPitch = 24.0f * accelerationBoost * closeRangeSpeedBoost;    // Снижено с 28

          float moveYaw = MathHelper.clamp(yawDelta, -speedYaw, speedYaw);
          float movePitch = MathHelper.clamp(pitchDelta, -speedPitch, speedPitch);

          // === CYPHRONE ENGINE: SMOOTH ACCELERATION DAMPING ===
          // Плавное затухание без резких скачков (убрали случайность)
          long currentTime = System.currentTimeMillis();
          if (currentTime - lastAccelerationUpdate > 100) {
               accelerationFactor = 0.98f; // Стабильное значение для плавности
               lastAccelerationUpdate = currentTime;
          }
          moveYaw *= accelerationFactor;
          movePitch *= accelerationFactor;

          float finalYaw = currentAngle.getYaw() + moveYaw;
          float finalPitch = MathHelper.clamp(currentAngle.getPitch() + movePitch, -90F, 90F);

          // === CYPHRONE ENGINE: MINIMAL PERLIN NOISE ===
          // Очень плавное хаотичное движение через шум Перлина (МИНИМАЛЬНОЕ)
          float perlinValue = getPerlinNoise(noiseOffset += 0.02f * PHI);
          float perlinJitterYaw = perlinValue * 0.05f;    // Снижено с 0.08
          float perlinJitterPitch = (float) Math.sin(noiseOffset * PI) * 0.03f; // Снижено с 0.06

          // === CYPHRONE ENGINE: SMOOTH MODULATION ===
          // Плавная модуляция без хаотичных скачков
          float smoothModulation = (float) Math.sin((System.currentTimeMillis() % 5000) / 5000.0 * PI * 2) * 0.015f;
          
          // === CYPHRONE ENGINE: SUBTLE JITTER FOR REALISM ===
          // Небольшой джиттер для реалистичности (еще ниже при ударе)
          float rotationDifference = (float) Math.hypot(yawDelta, pitchDelta);
          float subtleJitterYaw = 0;
          float subtleJitterPitch = 0;
          if (rotationDifference > 0.4f) {
               double time = (System.currentTimeMillis() % 10000) / 1000.0;
               subtleJitterYaw = (float) (Math.sin(time * PHI) * 0.03f);  // Снижено с 0.06
               subtleJitterPitch = (float) (Math.cos(time * (PHI * PHI)) * 0.025f); // Снижено с 0.05
          }

          // === CYPHRONE ENGINE: ADVANCED MATHEMATICAL SYSTEMS ===
          // Активные математические системы (ТОЛЬКО ПЛАВНЫЕ):
          // - Множество Мандельброта: фрактальная геометрия (ОЧЕНЬ редко)
          // - Множество Жюлиа: параметрическое исследование (ОЧЕНЬ редко)
          // - Геометрия Римана: неевклидова геометрия (ОЧЕНЬ редко)
          // - Фазовое пространство: многомерный анализ (ОЧЕНЬ редко)

          // === CYPHRONE ENGINE: MINIMAL MATHEMATICAL INFLUENCE ===
          // Минимальное влияние - только самые плавные формулы

          // === CYPHRONE ENGINE: MANDELBROT SET INFLUENCE ===
          // Фрактальная геометрия (ОЧЕНЬ редко, ОЧЕНЬ мало)
          if (houseCounter % 200 == 0) {
               double mandelbrotValue = calculateMandelbrot(yawDelta / 45.0, pitchDelta / 45.0, 6);
               float mandelbrotInfluence = (float) (mandelbrotValue / 6.0) * 0.0008f;
               finalYaw += mandelbrotInfluence;
          }

          // === CYPHRONE ENGINE: JULIA SET INFLUENCE ===
          // Параметрическое исследование (ОЧЕНЬ редко, ОЧЕНЬ мало)
          if (houseCounter % 250 == 0) {
               double juliaValue = calculateJuliaSet(yawDelta / 45.0, pitchDelta / 45.0, 8);
               float juliaInfluence = (float) (juliaValue / 8.0) * 0.0007f;
               finalPitch += juliaInfluence;
          }

          // === CYPHRONE ENGINE: RIEMANNIAN GEOMETRY INFLUENCE ===
          // Неевклидова геометрия (ОЧЕНЬ редко, ОЧЕНЬ мало)
          if (houseCounter % 300 == 0) {
               double riemannCurvature = calculateRiemannianCurvature(yawDelta, pitchDelta);
               float riemannInfluence = (float) (1.0 / (riemannCurvature + 1.0)) * 0.0006f;
               finalYaw += riemannInfluence;
          }

          // === CYPHRONE ENGINE: PHASE SPACE INFLUENCE ===
          // Многомерный анализ (ОЧЕНЬ редко, ОЧЕНЬ мало)
          if (houseCounter % 350 == 0) {
               double phaseDistance = calculatePhaseSpaceDistance(yawDelta, pitchDelta, lastYawDelta, lastPitchDelta);
               float phaseInfluence = (float) Math.tanh(phaseDistance / 100.0) * 0.0005f;
               finalPitch += phaseInfluence;
          }

          // === CYPHRONE ENGINE: SMOOTH WAVE MODULATION ===
          // Плавная волна для циклического движения (ОЧЕНЬ мало)
          wavePhase += 0.01f;
          if (houseCounter % 150 == 0) {
               float sineWave = (float) Math.sin(wavePhase) * 0.0008f;
               finalYaw += sineWave;
          }

          // === CYPHRONE ENGINE: EXPONENTIAL SMOOTHING ===
          // Экспоненциальное сглаживание (ОЧЕНЬ мало)
          if (houseCounter % 200 == 0) {
               float alpha = 0.2f;
               float smoothed = alpha * yawDelta + (1 - alpha) * previousYaw;
               previousYaw = smoothed;
               finalYaw += (smoothed - yawDelta) * 0.0002f;
          }

          // === CYPHRONE ENGINE: ROTATION VALIDATOR ===
          // Валидация поворота для предотвращения экстремальных скачков
          float maxYawStep = 70.0f;      // Повышено с 60
          float maxPitchStep = 50.0f;    // Повышено с 40

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

          // === CYPHRONE ENGINE: APPLY SMOOTH MODULATION ===
          finalYaw += perlinJitterYaw + smoothModulation + subtleJitterYaw;
          finalPitch = MathHelper.clamp(finalPitch + perlinJitterPitch + subtleJitterPitch, -90F, 90F);

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
