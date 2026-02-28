package fun.aegis.utils.features.aura.rotations.impl;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.util.hit.HitResult;
import fun.aegis.utils.display.interfaces.QuickImports;

/**
 * CYPHRONE ENGINE: Power Scan System
 * Advanced hitbox scanning for maximum range targeting
 * 
 * Сканирует все точки хитбокса и выбирает ближайшую по геометрии
 * Намеренно кушает производительность для точности
 */
public class PowerScan implements QuickImports {
     
     // Точки сканирования на теле
     private static final float[] HEAD_OFFSETS = {0, 0.85f, 0};
     private static final float[] CHEST_OFFSETS = {0, 0.5f, 0};
     private static final float[] LEGS_OFFSETS = {0, 0.1f, 0};
     private static final float[] FEET_OFFSETS = {0, 0, 0};
     
     // Кэш для оптимизации
     private LivingEntity lastTarget = null;
     private Vec3d lastBestPoint = null;
     private long lastScanTime = 0;
     private static final long SCAN_INTERVAL = 150; // мс между сканами (увеличено с 50)

     /**
      * CYPHRONE ENGINE: Power Scan Core
      * Сканирует все точки хитбокса и возвращает ближайшую
      */
     public Vec3d scanBestHitpoint(LivingEntity target) {
          if (target == null || mc.player == null) {
               return target != null ? target.getPos() : new Vec3d(0, 0, 0);
          }

          // Кэширование результатов для производительности
          long currentTime = System.currentTimeMillis();
          if (target == lastTarget && currentTime - lastScanTime < SCAN_INTERVAL) {
               return lastBestPoint;
          }

          lastTarget = target;
          lastScanTime = currentTime;

          Vec3d playerEyePos = mc.player.getEyePos();
          Box targetBox = target.getBoundingBox();
          
          // === CYPHRONE ENGINE: HITBOX POINT GENERATION ===
          // Генерируем все точки для сканирования
          Vec3d[] scanPoints = generateScanPoints(target, targetBox);
          
          // === CYPHRONE ENGINE: VISIBILITY CHECK ===
          // Проверяем видимость каждой точки
          Vec3d bestPoint = scanPoints[0];
          double minDistance = Double.MAX_VALUE;
          
          for (Vec3d point : scanPoints) {
               // Raycast для проверки видимости
               if (isPointVisible(playerEyePos, point)) {
                    // Вычисляем расстояние до точки
                    double distance = playerEyePos.distanceTo(point);
                    
                    // Выбираем ближайшую видимую точку
                    if (distance < minDistance) {
                         minDistance = distance;
                         bestPoint = point;
                    }
               }
          }

          lastBestPoint = bestPoint;
          return bestPoint;
     }

     /**
      * CYPHRONE ENGINE: Scan Point Generator
      * Генерирует точки сканирования на теле цели
      * Оптимизировано: 8 точек вместо 16 для меньшего дёрганья
      */
     private Vec3d[] generateScanPoints(LivingEntity target, Box box) {
          Vec3d targetPos = target.getPos();
          float height = target.getHeight();
          
          // === CYPHRONE ENGINE: OPTIMIZED SCANNING ===
          // 8 точек = баланс между точностью и производительностью
          Vec3d[] points = new Vec3d[8];
          int index = 0;

          // Голова (2 точки)
          points[index++] = targetPos.add(0.15f, height * 0.95f, 0.15f);
          points[index++] = targetPos.add(-0.15f, height * 0.95f, -0.15f);

          // Туловище (2 точки)
          points[index++] = targetPos.add(0.1f, height * 0.5f, 0.1f);
          points[index++] = targetPos.add(-0.1f, height * 0.5f, -0.1f);

          // Ноги (2 точки - самые ближайшие по геометрии издалека)
          points[index++] = targetPos.add(0.1f, height * 0.15f, 0.1f);
          points[index++] = targetPos.add(-0.1f, height * 0.15f, -0.1f);

          // Центр (2 точки - резервные)
          points[index++] = targetPos.add(0, height * 0.5f, 0);
          points[index++] = targetPos.add(0, height * 0.25f, 0);

          return points;
     }

     /**
      * CYPHRONE ENGINE: Visibility Checker
      * Проверяет видимость точки через raycast
      * Это дорогая операция - намеренно
      */
     private boolean isPointVisible(Vec3d from, Vec3d to) {
          if (mc.world == null) return false;
          
          HitResult result = mc.world.raycast(new RaycastContext(
               from,
               to,
               RaycastContext.ShapeType.COLLIDER,
               RaycastContext.FluidHandling.NONE,
               mc.player
          ));

          return result.getType() == HitResult.Type.MISS;
     }

     /**
      * CYPHRONE ENGINE: Distance Optimization
      * Выбирает точку на основе расстояния для максимального range
      * На дальних дистанциях предпочитает ноги
      */
     public Vec3d optimizeForDistance(LivingEntity target, Vec3d[] scanPoints) {
          if (mc.player == null) return scanPoints[0];

          double distance = mc.player.distanceTo(target);
          
          // На дальних дистанциях (> 4 блоков) целимся в ноги
          if (distance > 4.0) {
               // Ноги - индексы 4-5
               return findClosestVisiblePoint(scanPoints, 4, 6);
          }
          
          // На средних дистанциях (2-4 блока) целимся в туловище
          if (distance > 2.0) {
               // Туловище - индексы 2-3
               return findClosestVisiblePoint(scanPoints, 2, 4);
          }
          
          // На близких дистанциях целимся в голову
          // Голова - индексы 0-1
          return findClosestVisiblePoint(scanPoints, 0, 2);
     }

     /**
      * CYPHRONE ENGINE: Closest Point Finder
      * Находит ближайшую видимую точку в диапазоне
      */
     private Vec3d findClosestVisiblePoint(Vec3d[] points, int start, int end) {
          Vec3d playerEyePos = mc.player.getEyePos();
          Vec3d closest = points[start];
          double minDist = Double.MAX_VALUE;

          for (int i = start; i < end && i < points.length; i++) {
               if (isPointVisible(playerEyePos, points[i])) {
                    double dist = playerEyePos.distanceTo(points[i]);
                    if (dist < minDist) {
                         minDist = dist;
                         closest = points[i];
                    }
               }
          }

          return closest;
     }

     /**
      * CYPHRONE ENGINE: Performance Cost
      * Эта система намеренно кушает производительность
      * Но это обоснованно - она реально много вычисляет
      */
     public void clearCache() {
          lastTarget = null;
          lastBestPoint = null;
          lastScanTime = 0;
     }
}
