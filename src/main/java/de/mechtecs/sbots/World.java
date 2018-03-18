package de.mechtecs.sbots;

import de.mechtecs.sbots.math.Float64VectorCustom;

import java.util.ArrayList;
import java.util.stream.Collectors;


import static de.mechtecs.sbots.Constants.*;
import static de.mechtecs.sbots.Helpers.*;
import static java.lang.Math.*;

public class World {

    public int[] numCarnivore;
    public int[] numHerbivore;
    public int ptr;

    private int modcounter = 0;
    private int current_epoch = 0;
    private int idcounter = 0;

    private ArrayList<Agent> agents;

    // food
    private int FW = WIDTH/CZ;
    private int FH = HEIGHT/CZ;
    private int fx;
    private int fy;
    private float food[][] = new float[WIDTH/CZ][HEIGHT/CZ];
    public boolean CLOSED = false; //if environment is closed, then no random bots are added per time interval

    public World()
    {
        addRandomBots(NUMBOTS);
        //inititalize food layer

        for (int x=0;x<FW;x++) {
            for (int y=0;y<FH;y++) {
                food[x][y]= 0;
            }
        }

        numCarnivore=new int[200];
        numHerbivore=new int[200];

        ptr=0;
    }

    public void update()
    {
        modcounter++;

        //Process periodic events
        //Age goes up!
        if (modcounter%100==0) {
            for (int i=0;i<agents.size();i++) {
                agents.get(i).age++;    //agents age...
            }
        }

        if(modcounter%1000==0){
            int[] num_herbs_carns = numHerbCarnivores();
            numHerbivore[ptr]= num_herbs_carns[0];
            numCarnivore[ptr]= num_herbs_carns[1];
            ptr++;
            if(ptr == numHerbivore.length) ptr = 0;
        }
        if (modcounter%1000==0) writeReport();
        if (modcounter>=10000) {
            modcounter=0;
            current_epoch++;
        }
        if (modcounter%FOODADDFREQ==0) {
            fx=randi(0,FW);
            fy=randi(0,FH);
            food[fx][fy]= FOODMAX;
        }

        //reset any counter variables per agent
        for(int i=0;i<agents.size();i++){
            agents.get(i).spiked= false;
        }

        //give input to every agent. Sets in[] array
        setInputs();

        //brains tick. computes in[] . out[]
        brainsTick();

        //read output and process consequences of bots on environment. requires out[]
        processOutputs();

        //process bots: health and deaths
        for (int i=0;i<agents.size();i++) {
            float baseloss= 0.0002f; // + 0.0001*(abs(agents.get(i).w1) + abs(agents.get(i).w2))/2;
            //if (agents.get(i).w1<0.1 && agents.get(i).w2<0.1) baseloss=0.0001; //hibernation :p
            //baseloss += 0.00005*agents.get(i).soundmul; //shouting costs energy. just a tiny bit

            if (agents.get(i).boost) {
                //boost carries its price, and it's pretty heavy!
                agents.get(i).health -= baseloss*BOOSTSIZEMULT*1.3;
            } else {
                agents.get(i).health -= baseloss;
            }
        }

        //process temperature preferences
        for (int i=0;i<agents.size();i++) {

            //calculate temperature at the agents spot. (based on distance from equator)
            float dd= (float) (2.0*abs(agents.get(i).pos.get(0).doubleValue()/WIDTH - 0.5));
            float discomfort= abs(dd-agents.get(i).temperature_preference);
            discomfort= discomfort*discomfort;
            if (discomfort<0.08) discomfort=0;
            agents.get(i).health -= TEMPERATURE_DISCOMFORT*discomfort;
        }

        //process indicator (used in drawing)
        for (int i=0;i<agents.size();i++){
            if(agents.get(i).indicator>0) agents.get(i).indicator -= 1;
        }

        //remove dead agents.
        //first distribute foods
        for (int i=0;i<agents.size();i++) {
            //if this agent was spiked this round as well (i.e. killed). This will make it so that
            //natural deaths can't be capitalized on. I feel I must do this or otherwise agents
            //will sit on spot and wait for things to die around them. They must do work!
            if (agents.get(i).health<=0 && agents.get(i).spiked) {

                //distribute its food. It will be erased soon
                //first figure out how many are around, to distribute this evenly
                int numaround=0;
                for (int j=0;j<agents.size();j++) {
                    if (agents.get(j).health>0) {
                        float d= (agents.get(i).pos.minus(agents.get(j).pos)).length();
                        if (d<FOOD_DISTRIBUTION_RADIUS) {
                            numaround++;
                        }
                    }
                }

                //young killed agents should give very little resources
                //at age 5, they mature and give full. This can also help prevent
                //agents eating their young right away
                float agemult= 1.0f;
                if(agents.get(i).age<5) agemult= (float) (agents.get(i).age*0.2);

                if (numaround>0) {
                    //distribute its food evenly
                    for (int j=0;j<agents.size();j++) {
                        if (agents.get(j).health>0) {
                            float d= (agents.get(i).pos.minus(agents.get(j).pos)).length();
                            if (d<FOOD_DISTRIBUTION_RADIUS) {
                                agents.get(j).health += 5*(1-agents.get(j).herbivore)*(1-agents.get(j).herbivore)/pow(numaround,1.25)*agemult;
                                agents.get(j).repcounter -= REPMULT*(1-agents.get(j).herbivore)*(1-agents.get(j).herbivore)/pow(numaround,1.25)*agemult; //good job, can use spare parts to make copies
                                if (agents.get(j).health>2) agents.get(j).health=2; //cap it!
                                agents.get(j).initEvent(30,1,1,1); //white means they ate! nice
                            }
                        }
                    }
                }

            }
        }

        // remove all the dead agents >:)
        this.agents = this.agents.parallelStream().filter(agent -> agent.health > 0).collect(Collectors.toCollection(ArrayList::new));

        //handle reproduction
        for (int i=0;i<agents.size();i++) {
            if (agents.get(i).repcounter<0 && agents.get(i).health>0.65 && modcounter%15==0 && randf(0,1)<0.1) { //agent is healthy and is ready to reproduce. Also inject a bit non-determinism
                //agents.get(i).health= 0.8; //the agent is left vulnerable and weak, a bit
                reproduce(i, agents.get(i).MUTRATE1, agents.get(i).MUTRATE2); //this adds BABIES new agents to agents[]
                agents.get(i).repcounter= agents.get(i).herbivore*randf(REPRATEH-0.1,REPRATEH+0.1) + (1-agents.get(i).herbivore)*randf(REPRATEC-0.1,REPRATEC+0.1);
            }
        }

        //add new agents, if environment isn't closed
        if (!CLOSED) {
            //make sure environment is always populated with at least NUMBOTS bots
            if (agents.size()<NUMBOTS
                    ) {
                //add new agent
                addRandomBots(1);
            }
            if (modcounter%100==0) {
                if (randf(0,1)<0.5){
                    addRandomBots(1); //every now and then add random bots in
                }else
                    addNewByCrossover(); //or by crossover
            }
        }


    }

    void setInputs()
    {
        //P1 R1 G1 B1 FOOD P2 R2 G2 B2 SOUND SMELL HEALTH P3 R3 G3 B3 CLOCK1 CLOCK 2 HEARING     BLOOD_SENSOR   TEMPERATURE_SENSOR
        //0   1  2  3  4   5   6  7 8   9     10     11   12 13 14 15 16       17      18           19                 20

        float PI8= (float) (Math.PI/8/2); //pi/8/2
        float PI38= 3*PI8; //3pi/8/2
        float PI4= (float) (Math.PI/4);

        for (int i=0;i<agents.size();i++) {
            Agent a= agents.get(i);

            //HEALTH
            a.in.set(11,  cap(a.health/2)); //divide by 2 since health is in [0,2]

            //FOOD
            int cx= (int) a.pos.getValue(0)/CZ;
            int cy= (int) a.pos.getValue(1)/CZ;
            a.in.set(4,  food[cx][cy]/FOODMAX);

            //SOUND SMELL EYES
            float[] p = new float[NUMEYES]; // proximity
            float[] r = new float[NUMEYES]; // red
            float[] g = new float[NUMEYES]; // green
            float[] b = new float[NUMEYES]; // blue

            float soaccum=0;
            float smaccum=0;
            float hearaccum=0;

            //BLOOD ESTIMATOR
            float blood= 0;

            for (int j=0;j<agents.size();j++) {
                if (i==j) continue;
                Agent a2= agents.get(j);

                if (a.pos.getValue(0)<a2.pos.getValue(0)-DIST || a.pos.getValue(0)>a2.pos.getValue(0)+DIST
                        || a.pos.getValue(1)>a2.pos.getValue(1)+DIST || a.pos.getValue(1)<a2.pos.getValue(1)-DIST) continue;

                float d= (a.pos.minus(a2.pos)).length();

                if (d<DIST) {

                    //smell
                    smaccum+= (DIST-d)/DIST;

                    //sound
                    soaccum+= (DIST-d)/DIST*(max(abs(a2.w1),abs(a2.w2)));

                    //hearing. Listening to other agents
                    hearaccum+= a2.soundmul*(DIST-d)/DIST;

                    float ang= (a2.pos.minus(a.pos)).get_angle(); //current angle between bots

                    for(int q=0;q<NUMEYES;q++){
                        float aa = (float) (a.angle + a.eyedir.getValue(q));
                        if (aa<-Math.PI) aa += 2*Math.PI;
                        if (aa>Math.PI) aa -= 2*Math.PI;

                        float diff1= aa- ang;
                        if (abs(diff1)>Math.PI) diff1= (float) (2*Math.PI- abs(diff1));
                        diff1= abs(diff1);

                        float fov = (float) a.eyefov.getValue(q);
                        if (diff1<fov) {
                            //we see a2 with this eye. Accumulate stats
                            float mul1= a.eyesensmod*(abs(fov-diff1)/fov)*((DIST-d)/DIST);
                            p[q] += mul1*(d/DIST);
                            r[q] += mul1*a2.red;
                            g[q] += mul1*a2.gre;
                            b[q] += mul1*a2.blu;
                        }
                    }

                    //blood sensor
                    float forwangle= a.angle;
                    float diff4= forwangle- ang;
                    if (abs(forwangle)>Math.PI) diff4= (float) (2*Math.PI- abs(forwangle));
                    diff4= abs(diff4);
                    if (diff4<PI38) {
                        float mul4= ((PI38-diff4)/PI38)*((DIST-d)/DIST);
                        //if we can see an agent close with both eyes in front of us
                        blood+= mul4*(1-agents.get(j).health/2); //remember: health is in [0 2]
                        //agents with high life dont bleed. low life makes them bleed more
                    }
                }
            }

            smaccum *= a.smellmod;
            soaccum *= a.soundmod;
            hearaccum *= a.hearmod;
            blood *= a.bloodmod;

            a.in.set(0,  cap(p[0]));
            a.in.set(1,  cap(r[0]));
            a.in.set(2,  cap(g[0]));
            a.in.set(3,  cap(b[0]));

            a.in.set(5,  cap(p[1]));
            a.in.set(6,  cap(r[1]));
            a.in.set(7,  cap(g[1]));
            a.in.set(8,  cap(b[1]));
            a.in.set(9,  cap(soaccum));
            a.in.set(10,  cap(smaccum));

            a.in.set(12,  cap(p[2]));
            a.in.set(13,  cap(r[2]));
            a.in.set(14,  cap(g[2]));
            a.in.set(15,  cap(b[2]));
            a.in.set(16, (float) abs(sin(modcounter/a.clockf1)));
            a.in.set(17, (float) abs(sin(modcounter/a.clockf2)));
            a.in.set(18,  cap(hearaccum));
            a.in.set(19,  cap(blood));

            //temperature varies from 0 to 1 across screen.
            //it is 0 at equator (in middle), and 1 on edges. Agents can sense discomfort
            float dd= (float) (2.0*abs(a.pos.getValue(0)/WIDTH - 0.5));
            float discomfort= abs(dd - a.temperature_preference);
            a.in.set(20,  discomfort);

            a.in.set(21,  cap(p[3]));
            a.in.set(22,  cap(r[3]));
            a.in.set(23,  cap(g[3]));
            a.in.set(24,  cap(b[3]));

        }
    }

    void processOutputs()
    {
        //assign meaning
        //LEFT RIGHT R G B SPIKE BOOST SOUND_MULTIPLIER GIVING
        // 0    1    2 3 4   5     6         7             8
        for (int i=0;i<agents.size();i++) {
            Agent a= agents.get(i);
            a.red= (float) a.out.getValue(2);
            a.gre= (float) a.out.getValue(3);
            a.blu= (float) a.out.getValue(4);
            a.w1= (float) a.out.getValue(0); //-(2*(float) a.out.getValue(0)-1);
            a.w2= (float) a.out.getValue(1); //-(2*(float) a.out.getValue(1)-1);
            a.boost= (float) a.out.getValue(6)>0.5;
            a.soundmul= (float) a.out.getValue(7);
            a.give= (float) a.out.getValue(8);

            //spike length should slowly tend towards out[5]
            float g= (float) a.out.getValue(5);
            if (a.spikeLength<g)
                a.spikeLength+=SPIKESPEED;
            else if (a.spikeLength>g)
                a.spikeLength= g; //its easy to retract spike, just hard to put it up
        }

        //move bots
        //#pragma omp parallel for
        for (int i=0;i<agents.size();i++) {
            Agent a= agents.get(i);

            Float64VectorCustom v = Float64VectorCustom.valueOf(BOTRADIUS/2, 0);
            v = v.rotate(a.angle + Math.PI/2);

            Float64VectorCustom w1p= a.pos.plus(v); //wheel positions
            Float64VectorCustom w2p= a.pos.minus(v);

            float BW1= BOTSPEED*a.w1;
            float BW2= BOTSPEED*a.w2;
            if (a.boost) {
                BW1=BW1*BOOSTSIZEMULT;
            }
            if (a.boost) {
                BW2=BW2*BOOSTSIZEMULT;
            }

            //move bots
            Float64VectorCustom vv= w2p.minus(a.pos);
            vv.rotate(-BW1);
            a.pos= w2p.minus(vv);
            a.angle -= BW1;
            if (a.angle<-Math.PI) a.angle= (float) (Math.PI - (-Math.PI-a.angle));
            vv= a.pos.minus(w1p);
            vv.rotate(BW2);
            a.pos= w1p.plus(vv);
            a.angle += BW2;
            if (a.angle>Math.PI) a.angle= (float) (-Math.PI + (a.angle-Math.PI));

            //wrap around the map
            if (a.pos.getValue(0)<0) a.pos.set(0, (float) (WIDTH+a.pos.getValue(0)));
            if (a.pos.getValue(0)>=WIDTH) a.pos.set(0, (float) (a.pos.getValue(0)-WIDTH));
            if (a.pos.getValue(1)<0) a.pos.set(1, (float) (HEIGHT+a.pos.getValue(1)));
            if (a.pos.getValue(1)>=HEIGHT) a.pos.set(1, (float) (a.pos.getValue(1)-HEIGHT));
        }

        //process food intake for herbivors
        for (int i=0;i<agents.size();i++) {

            int cx= (int) agents.get(i).pos.getValue(0)/CZ;
            int cy= (int) agents.get(i).pos.getValue(1)/CZ;
            float f= food[cx][cy];
            if (f>0 && agents.get(i).health<2) {
                //agent eats the food
                float itk=min(f,FOODINTAKE);
                float speedmul= (float) ((1-(abs(agents.get(i).w1)+abs(agents.get(i).w2))/2)*0.7 + 0.3);
                itk= itk*agents.get(i).herbivore*speedmul; //herbivores gain more from ground food
                agents.get(i).health+= itk;
                agents.get(i).repcounter -= 3*itk;
                food[cx][cy]-= min(f,FOODWASTE);
            }
        }

        //process giving and receiving of food
        for (int i=0;i<agents.size();i++) {
            agents.get(i).dfood=0;
        }
        for (int i=0;i<agents.size();i++) {
            if (agents.get(i).give>0.5) {
                for (int j=0;j<agents.size();j++) {
                    float d= (agents.get(i).pos.minus(agents.get(j).pos)).length();
                    if (d<FOOD_SHARING_DISTANCE) {
                        //initiate transfer
                        if (agents.get(j).health<2) agents.get(j).health += FOODTRANSFER;
                        agents.get(i).health -= FOODTRANSFER;
                        agents.get(j).dfood += FOODTRANSFER; //only for drawing
                        agents.get(i).dfood -= FOODTRANSFER;
                    }
                }
            }
        }

        //process spike dynamics for carnivors
        if (modcounter%2==0) { //we dont need to do this TOO often. can save efficiency here since this is n^2 op in #agents
            for (int i=0;i<agents.size();i++) {

                //NOTE: herbivore cant attack. TODO: hmmmmm
                //fot now ok: I want herbivores to run away from carnivores, not kill them back
                if(agents.get(i).herbivore>0.8 || agents.get(i).spikeLength<0.2 || agents.get(i).w1<0.5 || agents.get(i).w2<0.5) continue;

                for (int j=0;j<agents.size();j++) {

                    if (i==j) continue;
                    float d= (agents.get(i).pos.minus(agents.get(j).pos)).length();

                    if (d<2*BOTRADIUS) {
                        //these two are in collision and agent i has extended spike and is going decent fast!
                        Float64VectorCustom v = Float64VectorCustom.valueOf(1,0);
                        v.rotate(agents.get(i).angle);
                        float diff= v.angle_between(agents.get(j).pos.minus(agents.get(i).pos));
                        if (abs(diff)<Math.PI/8) {
                            //bot i is also properly aligned!!! that's a hit
                            float mult=1;
                            if (agents.get(i).boost) mult= BOOSTSIZEMULT;
                            float DMG= SPIKEMULT*agents.get(i).spikeLength*max(abs(agents.get(i).w1),abs(agents.get(i).w2))*BOOSTSIZEMULT;

                            agents.get(j).health-= DMG;

                            if (agents.get(i).health>2) agents.get(i).health=2; //cap health at 2
                            agents.get(i).spikeLength= 0; //retract spike back down

                            agents.get(i).initEvent(40*DMG,1,1,0); //yellow event means bot has spiked other bot. nice!

                            Float64VectorCustom v2 = Float64VectorCustom.valueOf(1,0);
                            v2.rotate(agents.get(j).angle);
                            float adiff= v.angle_between(v2);
                            if (abs(adiff)<Math.PI/2) {
                                //this was attack from the back. Retract spike of the other agent (startle!)
                                //this is done so that the other agent cant right away "by accident" attack this agent
                                agents.get(j).spikeLength= 0;
                            }

                            agents.get(j).spiked= true; //set a flag saying that this agent was hit this turn
                        }
                    }
                }
            }
        }
    }

    // Multi-Threaded
    void brainsTick()
    {
        this.agents.parallelStream().forEach(a -> a.tick());
    }

    void addRandomBots(int num)
    {
        for (int i=0;i<num;i++) {
            Agent a;
            a.id= idcounter;
            idcounter++;
            agents.push_back(a);
        }
    }

    void positionOfInterest(int type, float &xi, float &yi) {
        if(type==1){
            //the interest of type 1 is the oldest agent
            int maxage=-1;
            int maxi=-1;
            for(int i=0;i<agents.size();i++){
                if(agents.get(i).age>maxage) { maxage = agents.get(i).age; maxi=i; }
            }
            if(maxi!=-1) {
                xi = agents[maxi].pos.getValue(0);
                yi = agents[maxi].pos.getValue(1);
            }
        } else if(type==2){
            //interest of type 2 is the selected agent
            int maxi=-1;
            for(int i=0;i<agents.size();i++){
                if(agents.get(i).selectflag==1) {maxi=i; break; }
            }
            if(maxi!=-1) {
                xi = agents[maxi].pos.getValue(0);
                yi = agents[maxi].pos.getValue(1);
            }
        }

    }

    void addCarnivore()
    {
        Agent a;
        a.id= idcounter;
        idcounter++;
        a.herbivore= randf(0, 0.1);
        agents.push_back(a);
    }

    void addHerbivore()
    {
        Agent a;
        a.id= idcounter;
        idcounter++;
        a.herbivore= randf(0.9, 1);
        agents.push_back(a);
    }


    void addNewByCrossover()
    {

        //find two success cases
        int i1= randi(0, agents.size());
        int i2= randi(0, agents.size());
        for (int i=0;i<agents.size();i++) {
            if (agents.get(i).age > agents[i1].age && randf(0,1)<0.1) {
                i1= i;
            }
            if (agents.get(i).age > agents[i2].age && randf(0,1)<0.1 && i!=i1) {
                i2= i;
            }
        }

        Agent a1= &agents[i1];
        Agent a2= &agents[i2];


        //cross brains
        Agent anew = a1.crossover(*a2);


        //maybe do mutation here? I dont know. So far its only crossover
        anew.id= idcounter;
        idcounter++;
        agents.push_back(anew);
    }

    void reproduce(int ai, float MR, float MR2)
    {
        if (randf(0,1)<0.04) MR= MR*randf(1, 10);
        if (randf(0,1)<0.04) MR2= MR2*randf(1, 10);

        agents[ai].initEvent(30,0,0.8,0); //green event means agent reproduced.
        for (int i=0;i<BABIES;i++) {

            Agent a2 = agents[ai].reproduce(MR,MR2);
            a2.id= idcounter;
            idcounter++;
            agents.push_back(a2);

            //TODO fix recording
            //record this
            //FILE* fp = fopen("log.txt", "a");
            //fprintf(fp, "%i %i %i\n", 1, this.id, a2.id); //1 marks the event: child is born
            //fclose(fp);
        }
    }

    void writeReport()
    {
        //TODO fix reporting
        //save all kinds of nice data stuff
//     int numherb=0;
//     int numcarn=0;
//     int topcarn=0;
//     int topherb=0;
//     for(int i=0;i<agents.size();i++){
//         if(agents.get(i).herbivore>0.5) numherb++;
//         else numcarn++;
//
//         if(agents.get(i).herbivore>0.5 && agents.get(i).gencount>topherb) topherb= agents.get(i).gencount;
//         if(agents.get(i).herbivore<0.5 && agents.get(i).gencount>topcarn) topcarn= agents.get(i).gencount;
//     }
//
//     FILE* fp = fopen("report.txt", "a");
//     fprintf(fp, "%i %i %i %i\n", numherb, numcarn, topcarn, topherb);
//     fclose(fp);
    }


    void reset()
    {
        agents.clear();
        addRandomBots(NUMBOTS);
    }

    void setClosed(bool close)
    {
        CLOSED = close;
    }

    bool isClosed() const
    {
        return CLOSED;
    }


    void processMouse(int button, int state, int x, int y)
    {
        if (state==0) {
            float mind=1e10;
            float mini=-1;
            float d;

            for (int i=0;i<agents.size();i++) {
                d= pow(x-agents.get(i).pos.getValue(0),2)+pow(y-agents.get(i).pos.getValue(1),2);
                if (d<mind) {
                    mind=d;
                    mini=i;
                }
            }
            //toggle selection of this agent
            for (int i=0;i<agents.size();i++) agents.get(i).selectflag=false;
            agents[mini].selectflag= true;
            agents[mini].printSelf();
        }
    }

    void draw(View* view, bool drawfood)
    {
        //draw food
        if(drawfood) {
            for(int i=0;i<FW;i++) {
                for(int j=0;j<FH;j++) {
                    float f= 0.5*food[i][j]/FOODMAX;
                    view.drawFood(i,j,f);
                }
            }
        }

        //draw all agents
        vector<Agent>::const_iterator it;
        for ( it = agents.begin(); it != agents.end(); ++it) {
            view.drawAgent(*it);
        }

        view.drawMisc();
    }

    public int[] numHerbCarnivores()
    {
        int numherb=0;
        int numcarn=0;
        for (int i=0;i<agents.size();i++) {
            if (agents.get(i).herbivore>0.5) numherb++;
            else numcarn++;
        }

        return new int[]{numherb,numcarn};
    }

    int numAgents() const
    {
        return agents.size();
    }

    int epoch() const
    {
        return current_epoch;
    }

}
