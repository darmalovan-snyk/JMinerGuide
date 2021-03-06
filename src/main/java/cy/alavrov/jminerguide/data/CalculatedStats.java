/*
 * Copyright (c) 2015, Andrey Lavrov <lavroff@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package cy.alavrov.jminerguide.data;

import cy.alavrov.jminerguide.data.booster.BoosterShip;
import cy.alavrov.jminerguide.data.booster.ForemanLink;
import cy.alavrov.jminerguide.data.ship.HarvestUpgrade;
import cy.alavrov.jminerguide.data.ship.Hull;
import cy.alavrov.jminerguide.data.ship.MiningDrone;
import cy.alavrov.jminerguide.data.harvestable.HarvestableType;
import cy.alavrov.jminerguide.data.ship.Rig;
import cy.alavrov.jminerguide.data.ship.Ship;
import cy.alavrov.jminerguide.data.ship.Turret;
import cy.alavrov.jminerguide.data.character.EVECharacter;
import cy.alavrov.jminerguide.data.implant.Implant;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Stats, calculated for a ship, based on it's hull, modules, pilot and 
 * whatever else.
 *  
 * @author Andrey Lavrov <lavroff@gmail.com>
 */
public class CalculatedStats implements ICalculatedStats {
    
    /**
     * Used to sort rigs by their drone yield bonus, descending.
     */
    private final static Comparator<Rig> rigComparator = new Comparator<Rig>() {
        @Override
        public int compare(Rig o1, Rig o2) {
            return Integer.valueOf(o2.getDroneYieldBonus()).compareTo(o1.getDroneYieldBonus());
        }
    };
    
    /**
     * Yield of a turret, in m3.
     */
    private final float turretYield;
    
    /**
     * Yield of all turrets, combined, in m3.
     */
    private final float combinedTurretYield;
    
    /**
     * Cycle of a turret, in seconds.
     */
    private final float turretCycle;
    
    /**
     * Individual turret yield per second, in m3/sec.
     */
    private final float turretM3S;
    
    /**
     * Total turret yield per second, in m3/sec.
     */
    private final float combinedTurretM3S;
    
    /**
     * Yield of a drone, in m3.
     */
    private final float droneYield;
    
    /**
     * Yield of all drones, combined, in m3.
     */
    private final float combinedDroneYield;
    
    /**
     * Cycle of a drone, in seconds.
     */
    private final float droneCycle;
    
    /**
     * Total drone yield per second, in m3/sec.
     */
    private final float droneM3S;
    
    /**
     * Total ship yield, in m3/hour.
     */
    private final float totalM3H;
    
    /**
     * Ship's optimal, in metres.
     */
    private final int optimal;
    
    /**
     * Ship's ore (or cargo) hold, in m3.
     */
    private final int oreHold;
    
    /**
     * How long it takes for ore hold to fill, in seconds.
     */
    private final int secsForOreHold;
    
    /**
     * Link bonus to the cycle time, in percents.
     */
    private final float linkCycleBonus;
    
    /**
     * Link bonus to the mining turret optimal, in percents.
     */
    private final float linkOptimalBonus;
    
    public CalculatedStats(EVECharacter miner, EVECharacter booster, Ship ship, BoosterShip boosterShip, boolean mercoxit) {
        
        Turret turret = ship.getTurret();
        Hull hull = ship.getHull();
        Hull.BonusCalculationResult bonus = hull.calculateSkillBonusModificators(miner);
        
        int upgrades = ship.getHarvestUpgradeCount();
        HarvestUpgrade upgrade = ship.getHarvestUpgrade();
                
        
        // ATM there is only one mining mindlink, so we'll just hardcode it in.
        boolean haveMindlink = booster.getSlot10Implant() == Implant.MFMINDLINK;                
        
        ForemanLink cycleLink = boosterShip.getCycleLink();
        float baseCycleBonus = cycleLink.getCycleBonus();
        float effectiveCycleBonus = baseCycleBonus * boosterShip.getHull()
                .calculateBoostModifier(booster, boosterShip.isDeployedMode());
        effectiveCycleBonus = effectiveCycleBonus * booster.getBoosterLinkModifier();                
        
        ForemanLink optimalLink = boosterShip.getOptimalLink();
        float baseOptimalBonus = optimalLink.getOptimalBonus();
        float effectiveOptimalBonus = baseOptimalBonus * boosterShip.getHull()
                .calculateBoostModifier(booster, boosterShip.isDeployedMode());
        effectiveOptimalBonus = effectiveOptimalBonus * booster.getBoosterLinkModifier();
        
        
        if (haveMindlink) {
            effectiveCycleBonus = effectiveCycleBonus * 1.25f;
            effectiveOptimalBonus = effectiveOptimalBonus * 1.25f;
        }
        
        linkCycleBonus = effectiveCycleBonus;
        linkOptimalBonus = effectiveOptimalBonus;
        
        float effectiveCycleModifier = 1 - 0.01f*effectiveCycleBonus;
        float effectiveOptimalModifier = 1 + 0.01f*effectiveOptimalBonus;
        
        float baseTurretYield = turret.getBaseYield();
        float actualTurretYield;
        
        switch (turret.getTurretType()) {
            case MININGLASER:
            case STRIPMINER:
                actualTurretYield = baseTurretYield * 
                    (1 + hull.getRoleMiningYieldBonus()/100f) * 
                    bonus.miningYieldMod * miner.getMiningYieldModifier();
                
                if (upgrades > 0 ) {
                    for (int i = 0; i < upgrades; i++) {
                        actualTurretYield = actualTurretYield * 
                                (1 + upgrade.getOreYieldBonus() * 0.01f);
                    }
                }
                
                if (turret.isUsingCrystals()) {
                    if (turret.getHarvestableType() == HarvestableType.MERCOXIT && mercoxit) {
                        actualTurretYield = actualTurretYield * ship.getTurretCrystal().getMercMod();
                        actualTurretYield = actualTurretYield * (1 + 0.01f*ship.getRig1().getMercoxitYieldBonus());
                        actualTurretYield = actualTurretYield * (1 + 0.01f*ship.getRig2().getMercoxitYieldBonus());
                        if (hull.getRigSlots() > 2) {
                            actualTurretYield = actualTurretYield * (1 + 0.01f*ship.getRig3().getMercoxitYieldBonus());
                        }
                    } else {
                        actualTurretYield = actualTurretYield * ship.getTurretCrystal().getOreMod();
                    }
                }
                
                
                if (haveMindlink) {
                    actualTurretYield = actualTurretYield * 1.15f;
                } else {
                    actualTurretYield = actualTurretYield * (1 + 0.02f * booster.getSkillLevel(EVECharacter.SKILL_MINING_FOREMAN));
                }
                        
                break;
                
            case GASHARVESTER:
            // only bonus yield for gas is from hulls. 
                actualTurretYield = baseTurretYield * 
                    (1 + hull.getRoleGasYieldBonus()/100f);
                break;
                
            default:
            case ICEHARVESTER:
                actualTurretYield = baseTurretYield;
                // ice harvesters have no bonus to yield.
                break;         
        }
        
        turretYield = actualTurretYield;
        combinedTurretYield = turretYield * ship.getTurretCount();
        
        float baseTurretCycle = turret.getCycleDuration();
        float actualTurretCycle;
        
        switch (turret.getTurretType()) {
            default:
            case MININGLASER:                                    
                actualTurretCycle = baseTurretCycle;
                break;
                
            case STRIPMINER:
                actualTurretCycle = baseTurretCycle * bonus.stripCycleMod;
                break;
                
            case GASHARVESTER:
                actualTurretCycle = baseTurretCycle * bonus.gasCycleMod * 
                    miner.getGasCycleModifier();
                break;
                
            case ICEHARVESTER:
                actualTurretCycle = baseTurretCycle * 
                    (1 - hull.getRoleIceCycleBonus()/100f) *
                    bonus.stripCycleMod * miner.getIceCycleModifier();
                
                if (upgrades > 0 ) {
                    for (int i = 0; i < upgrades; i++) {
                        actualTurretCycle = actualTurretCycle * 
                                (1 - upgrade.getIceCycleBonus()* 0.01f);
                    }
                }
                
                actualTurretCycle = actualTurretCycle * (1 - 0.01f*ship.getRig1().getIceCycleBonus());
                actualTurretCycle = actualTurretCycle * (1 - 0.01f*ship.getRig2().getIceCycleBonus());
                if (hull.getRigSlots() > 2) {
                    actualTurretCycle = actualTurretCycle * (1 - 0.01f*ship.getRig3().getIceCycleBonus());
                }
                
                break;
        }
        
        actualTurretCycle = actualTurretCycle * effectiveCycleModifier;
        turretCycle = actualTurretCycle;
        
        turretM3S = turretYield/turretCycle;
        combinedTurretM3S = combinedTurretYield/turretCycle;
        
        MiningDrone drone = ship.getDrone();
        if (turret.getHarvestableType() == HarvestableType.GAS || 
                turret.getHarvestableType() == HarvestableType.ICE ||
                (turret.getHarvestableType() == HarvestableType.MERCOXIT && mercoxit) ||
                drone == MiningDrone.NOTHING) {
            droneYield = 0; 
            combinedDroneYield = 0;
            droneCycle = 0;  
            droneM3S = 0;
        } else {                        
            float droneEffectiveYield = drone.getBaseYield() * miner.getDroneYieldModifier();
                        
            // ok, here it is harder, due to diminishing returns on rigs.
            // we'll have to apply rigs with greater bonus first, so it's 
            // rig sorting time!            
            Rig[] rigs;
            if (hull.getRigSlots() > 2) {
                rigs = new Rig[] {ship.getRig1(), ship.getRig2(), ship.getRig3()};
            } else {
                rigs = new Rig[] {ship.getRig1(), ship.getRig2()};
            }
            
            Arrays.sort(rigs, rigComparator);
            
            droneEffectiveYield = droneEffectiveYield * (1 + 0.01f * rigs[0].getDroneYieldBonus());
            droneEffectiveYield = droneEffectiveYield * (1 + 0.01f * 0.87f * rigs[1].getDroneYieldBonus());
            if (hull.getRigSlots() > 2) {
                droneEffectiveYield = droneEffectiveYield * (1 + 0.01f * 0.57f * rigs[2].getDroneYieldBonus());
            }
            
            droneYield = droneEffectiveYield;
            
            combinedDroneYield = droneYield * ship.getDroneCount();
            droneCycle = drone.getCycleDuration(); // no bonus to this.
            droneM3S = combinedDroneYield / droneCycle;
        }
                                      
        float totalM3S = combinedTurretM3S + droneM3S;
                
        int baseOptimal = turret.getOptimalRange();
        int effectiveOptimal;
        
        switch (turret.getTurretType()) {
            case ICEHARVESTER:
            case STRIPMINER:
                effectiveOptimal = (int) (baseOptimal * bonus.stripOptimalMod);
                break;
                
            default:
                effectiveOptimal = baseOptimal;
        }
        
        effectiveOptimal = (int) (effectiveOptimal * effectiveOptimalModifier);
        
        optimal = effectiveOptimal;
        
        int baseOreHold = hull.getOreHold();
        int effectiveOreHold = (int) (baseOreHold * bonus.oreHoldMod);
        
        oreHold = effectiveOreHold;
        
        float secsForOreHoldF = oreHold / totalM3S;
        secsForOreHold = (int) secsForOreHoldF;
        
        int stationTripSecs = 0;
        if (!miner.isUsingHauler()) {
            stationTripSecs = miner.getStationTripSecs();
        }
        
        float totalMiningCycle = secsForOreHoldF + stationTripSecs;        
        float cyclesInHr = 60*60 / totalMiningCycle;
        
        totalM3H = oreHold * cyclesInHr;
    }

    /**
     * Yield of a turret, in m3.
     * @return the turretYield
     */
    @Override
    public float getTurretYield() {
        return turretYield;
    }

    /**
     * Yield of all turrets, combined, in m3.
     * @return the combinedTurretYield
     */
    @Override
    public float getCombinedTurretYield() {
        return combinedTurretYield;
    }

    /**
     * Cycle of a turret, in seconds.
     * @return the turretCycle
     */
    @Override
    public float getTurretCycle() {
        return turretCycle;
    }

    /**
     * Total individual yield per second, in m3/sec.
     * @return the turretM3S
     */
    @Override
    public float getTurretM3S() {
        return turretM3S;
    }   
    
    /**
     * Total turret yield per second, in m3/sec.
     * @return the combinedTurretM3S
     */
    @Override
    public float getCombinedTurretM3S() {
        return combinedTurretM3S;
    }

    /**
     * Yield of a drone, in m3.
     * @return the droneYield
     */
    @Override
    public float getDroneYield() {
        return droneYield;
    }

    /**
     * Yield of all drones, combined, in m3.
     * @return the combinedDroneYield
     */
    @Override
    public float getCombinedDroneYield() {
        return combinedDroneYield;
    }

    /**
     * Cycle of a drone, in seconds.
     * @return the droneCycle
     */
    @Override
    public float getDroneCycle() {
        return droneCycle;
    }

    /**
     * Total drone yield per second, in m3/sec.
     * @return the droneM3S
     */
    @Override
    public float getDroneM3S() {
        return droneM3S;
    }

    /**
     * Total ship yield, in m3/hour.
     * @return the totalM3H
     */
    @Override
    public float getTotalM3H() {
        return totalM3H;
    }

    /**
     * Ship's optimal, in metres.
     * @return the optimal
     */
    @Override
    public int getOptimal() {
        return optimal;
    }

    /**
     * Ship's ore (or cargo) hold, in m3.
     * @return the cargo
     */
    @Override
    public int getOreHold() {
        return oreHold;
    }

    /**
     * How long it takes for ore hold to fill, in seconds.
     * @return the secsForCargo
     */
    @Override
    public int getSecsForOreHold() {
        return secsForOreHold;
    }

    /**
     * Link bonus to the cycle time, in percents.
     * @return the linkCycleBonus
     */
    @Override
    public float getLinkCycleBonus() {
        return linkCycleBonus;
    }

    /**
     * Link bonus to the mining turret optimal, in percents.
     * @return the linkOptimalBonus
     */
    @Override
    public float getLinkOptimalBonus() {
        return linkOptimalBonus;
    }
}

