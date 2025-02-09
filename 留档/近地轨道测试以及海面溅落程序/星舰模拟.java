package org.program;

import krpc.client.Connection;
import krpc.client.RPCException;
import krpc.client.Stream;
import krpc.client.StreamException;
import krpc.client.services.SpaceCenter;

import java.io.IOException;

import static krpc.client.services.SpaceCenter.SASMode.STABILITY_ASSIST;

public class 星舰模拟 {
    public static void main(String[] args) throws IOException, RPCException, StreamException, InterruptedException {
        Connection 游戏连接 = Connection.newInstance("星舰模拟");
        SpaceCenter 太空中心 = SpaceCenter.newInstance(游戏连接);
        SpaceCenter.Vessel 星舰 = 太空中心.getActiveVessel();

// 设置遥测流
        // 用于获取当前的通用时间
        太空中心.getUT();
        //持续监听SpaceCenter对象的getUT方法返回值的变化
        Stream<Double> 世界时间 = 游戏连接.addStream(SpaceCenter.class, "getUT");
        //当前飞船所在星球表面的 参考系
        SpaceCenter.ReferenceFrame 参考系 = 星舰.getSurfaceReferenceFrame();
        //获取了当前飞船的 飞行状态，包括位置、速度等信息
        SpaceCenter.Flight 飞行状态 = 星舰.flight(参考系);
        //持续监听飞船的 平均高度 的变化。getMeanAltitude方法返回飞船在其轨道上的平均高度。
        Stream<Double> 平均高度 = 游戏连接.addStream(飞行状态, "getMeanAltitude");
        //监听飞船轨道上 远地点 的高度变化
        Stream<Double> 远地点 =
                游戏连接.addStream(星舰.getOrbit(), "getApoapsisAltitude");

//=======================发射阶段============================
        {
            //阶段设置
            boolean if发射阶段1=false;
            boolean if发射阶段2=false;
            boolean if发射阶段3=false;
            boolean if回收阶段1=false;
            boolean if回收阶段2=false;
            boolean if回收阶段3=false;
            boolean if回收阶段4=false;

            // 启动前设置
            星舰.getControl().setSAS(false);
            星舰.getControl().setRCS(true);
            星舰.getControl().setThrottle(1);//推力级别 0~1

            //第一阶段 垂直发射
            {
                Thread.sleep(1000);
                System.out.println("启动");
                星舰.getControl().activateNextStage();//空格键
                星舰.getAutoPilot().setAutoTune(true);
                星舰.getAutoPilot().setSASMode(STABILITY_ASSIST);
                星舰.getAutoPilot().engage();//启动了火箭的自动驾驶仪
                星舰.getAutoPilot().targetPitchAndHeading(90, 90);//俯仰角（pitch）和航向角（heading）
                Thread.sleep(1000);
            }
            //第一阶段 当前高度为10000 进入下一阶段
            {
                System.out.println("开始发射至10000M高度==============");
                while (true) {
                    //测试区/////////////////////

                    /////////////////////////////
                    //获取高度
                    if (星舰.flight(参考系).getMeanAltitude() > 10000) {
                        星舰.getControl().setThrottle(0.7f);//推力级别 0~1
                        if发射阶段1 = true;
                        break;
                    }
                }
            }
            //第二阶段 开始调整缓慢姿态 直到调整为45度角 进入下一阶段
            {
                System.out.println("开始姿态调整====================");
                while (if发射阶段1) {
                    int i = 0;
                    double 循环下一阶段的高度 = 10200;
                    while (i <= 45) {

                        if (星舰.flight(参考系).getMeanAltitude() > 循环下一阶段的高度) {
                            if ((90 - i) % 10 == 0) {
                                System.out.println("姿态角度：" + (90 - i));
                            }
                            星舰.getAutoPilot().targetPitchAndHeading(90 - i, 90);
                            i++;
                            循环下一阶段的高度 += 200 + 2 * i;
                            if (i == 45) {
                                break;
                            }
                        }
                    }
                    if (i == 45) {
                        System.out.println("姿态调整45度，完毕" );
                        星舰.getControl().setThrottle(1);
                        if发射阶段2 = true;
                        break;
                    }
                }
            }
            //第二阶段 一级引擎直到耗尽 分离并小功率启动第二阶段引擎
            {
                System.out.println("等待燃料耗尽==============");
                while (if发射阶段2) {
                    //监听第一级分离阶段飞船中 液体燃料 的数量变化
                    //星舰.resourcesInDecoupleStage(0,false).getNames()=[LiquidFuel, Oxidizer]
                    if (星舰.resourcesInDecoupleStage(0, false).amount("LiquidFuel") < 5) {
                        星舰.getControl().setThrottle(0);
                        星舰.getControl().activateNextStage();//空格键 分离一二级
                        星舰.getControl().setThrottle(0.3f);//推力级别 0~1
                        if发射阶段3 = true;
                        break;
                    }
                }
            }
            //第三阶段 35秒全功率燃烧 姿态改为横向滑翔
            {
                Thread.sleep(1000);
                System.out.println("燃烧38秒，逐渐改变姿态==============");
                星舰.getControl().setThrottle(1);//推力级别 0~1
                float i = 0;
                while (if发射阶段3) {
                    Thread.sleep(1000);
                    i += 1.225; //i*38=45
                    星舰.getAutoPilot().targetPitchAndHeading(45 - i, 90);
                    if (i >= 45) {
                        星舰.getControl().setThrottle(0);
                        星舰.getAutoPilot().targetPitchAndHeading(-30, 90);
                        System.out.println("燃烧结束，姿态调整为-45度========");
                        星舰.getControl().setBrakes(true);
                        if回收阶段1=true;
                        break;
                    }
                }
            }
            //第三阶段 等待滑翔至3000米 开始姿态调整 抵消水平速度
            {
                boolean if高度条件=false;
                float i = 0;
                while (if回收阶段1)
                {
                    if (星舰.flight(参考系).getMeanAltitude() < 3000)
                    {
                        if高度条件=true;
                        System.out.println("高度小于3000，开始姿态调整");
                        break;
                    }

                }

                while (if高度条件)
                {
                    星舰.getControl().setBrakes(false);
                    星舰.getControl().setThrottle(0.5f);
                    星舰.getAutoPilot().targetPitchAndHeading(135, 90);
                    //水平速度判断
                    if ((星舰.flight(星舰.getOrbit().getBody().getReferenceFrame()).getHorizontalSpeed()<=10))
                    {
                        System.out.println("水平速度抵消，回正姿态");
                        星舰.getControl().setThrottle(0);
                        星舰.getAutoPilot().targetPitchAndHeading(90, 90);
                        if回收阶段2=true;
                        break;
                    }
                }
            }
            //第四阶段 调整竖直姿态 准备着陆
            {
                boolean if点火1=false;
                double thrust = 星舰.getAvailableThrust() * 0.8;
                double gravity = 9.8;
                double 减速度 = (thrust - 星舰.getMass() * gravity) / 星舰.getMass();
                while (if回收阶段2)
                {
                    //double 当前垂直速度 = Double.parseDouble(String.valueOf(星舰.flight(星舰.getOrbit().getBody().getReferenceFrame()).getVerticalSpeed()));
                    double 高度 = 星舰.flight(星舰.getOrbit().getBody().getReferenceFrame()).getVerticalSpeed() * 星舰.flight(星舰.getOrbit().getBody().getReferenceFrame()).getVerticalSpeed() / (2 * 减速度);
                    if (星舰.flight(参考系).getMeanAltitude() - 高度 < 10) {
                        星舰.getControl().setThrottle(1);
                        System.out.println("第一次点火高度 "+ 高度);
                        if点火1=true;
                        break;
                    }
                }
                //下落速度回到0左右，进入下一阶段
                while (if点火1) {
                    double 当前垂直速度 = Double.parseDouble(String.valueOf(星舰.flight(星舰.getOrbit().getBody().getReferenceFrame()).getVerticalSpeed()));
                    if (当前垂直速度 > -6) {
                        星舰.getControl().setThrottle(0.3F);
                        Thread.sleep(300);
                        星舰.getControl().setThrottle(0);
                        System.out.println("引擎关闭,准备二次点火");
                        if回收阶段3=true;
                        break;
                    }
                }

            }
            //第五阶段 最终着陆阶段
            {
                boolean if缓慢着陆=false;
                boolean if完成着陆=false;
                double thrust;
                double gravity = 9.8;
                double 减速度;
                //二次计算点火距离并着陆
                while (if回收阶段3) {
                    thrust = 星舰.getAvailableThrust() *1;
                    减速度 = (thrust - 星舰.getMass() * gravity) / 星舰.getMass();
                    double 高度 = 星舰.flight(星舰.getOrbit().getBody().getReferenceFrame()).getVerticalSpeed() * 星舰.flight(星舰.getOrbit().getBody().getReferenceFrame()).getVerticalSpeed() / (2 * 减速度);
                    if (星舰.flight(参考系).getMeanAltitude() - 高度 < 10) {
                        System.out.println("第二次点火高度 "+ 高度);
                        星舰.getControl().setThrottle(1);
                        if回收阶段4 = true;
                        break;
                    }

                }
                //下落速度第二次回到0左右，进入下一阶段
                while (if回收阶段4) {
                    double 当前垂直速度 = Double.parseDouble(String.valueOf(星舰.flight(星舰.getOrbit().getBody().getReferenceFrame()).getVerticalSpeed()));
                    if (当前垂直速度 > -1) {
                        float 平衡推力 = 星舰.getMass() * 10 / (星舰.getAvailableThrust());
                        星舰.getControl().setThrottle((float) (平衡推力*0.7));
                        if缓慢着陆 = true;
                        System.out.println("缓慢着陆");
                        break;
                    }
                }
                //缓慢着陆
                while (if缓慢着陆) {
                    星舰.getAutoPilot().targetPitchAndHeading(90, 90);//左右角
                    double 当前垂直速度 = Double.parseDouble(String.valueOf(星舰.flight(星舰.getOrbit().getBody().getReferenceFrame()).getVerticalSpeed()));
                    float 平衡推力 = 星舰.getMass() * 10 / (星舰.getAvailableThrust());
                    // System.out.println(平衡推力);
                    if (当前垂直速度 < -2) {
                        星舰.getControl().setThrottle(平衡推力);
                    } else if (当前垂直速度 > -1) {
                        星舰.getControl().setThrottle((float) (平衡推力 * 0.5));
                    }
                    if (星舰.flight(参考系).getMeanAltitude() < 2) {
                        星舰.getControl().setThrottle(0);
                        System.out.println("着陆");
                        if完成着陆=true;
                        break;
                    }
                }
                //着陆完成
                while (if完成着陆)
                {
                    游戏连接.close();
                }
            }

        }

    }
}
