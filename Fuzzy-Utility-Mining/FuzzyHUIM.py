# -*- coding: utf-8 -*-
"""
Created on Tue Feb 28 22:43:42 2017

@author: Gaurav
"""

#print ("Enter number of Items-")
#ni=input()
#ni=int(ni)
#print ("Enter number of Transactions-")
#nt=input()
#nt=int(nt)
#Considering Three Fuzzy Logic States- LOW,MIDDLE,HIGH
import numpy as np
#trans_tab=np.zeros((nt,ni),dtype='int')
#for i in range(0,nt): 
#    for j in range(0,ni):
#        trans_tab[i,j]=input()  #Inputting quantity of each item in each transaction
profits=np.array([1,8,5,3]);
trans_tab=np.array([[11,0,0,0],[2,0,1,1],[0,1,0,0],[8,0,1,2],[6,0,0,2],[7,0,1,3],[0,2,3,0],[2,0,1,1],[0,1,0,0],[10,0,0,0]]);
low=1.0
middle=6.0
high=11.0

#profits=np.zeros(ni,dtype='float')
#print("Enter profits of each item")
#for i in range(0,ni):
#    profits[i]=input()
nt=10;
ni=4;
fuz_utility=np.zeros((nt,(ni*3)),dtype='float')
mem_values=np.zeros((nt,(ni*3)),dtype='float')
for i in range(0,nt):
    for j in range(0,ni):
        if trans_tab[i,j]==0:
            mem_values[i,(3*j)+0]=0
            mem_values[i,(3*j)+1]=0
            mem_values[i,(3*j)+2]=0
        if trans_tab[i,j]==low:
            mem_values[i,(3*j)+0]=1
            mem_values[i,(3*j)+1]=0
            mem_values[i,(3*j)+2]=0
        if trans_tab[i,j]==middle:
            mem_values[i,(3*j)+0]=0
            mem_values[i,(3*j)+1]=1
            mem_values[i,(3*j)+2]=0
        if trans_tab[i,j]>=high:
            mem_values[i,(3*j)+0]=0
            mem_values[i,(3*j)+1]=0
            mem_values[i,(3*j)+2]=1
        if (trans_tab[i,j]>low) and (trans_tab[i,j]<middle):
            mem_values[i,(3*j)+0]=(middle-trans_tab[i,j])/(middle-low)
            mem_values[i,(3*j)+1]=1-mem_values[i,(3*j)+0]
            mem_values[i,(3*j)+2]=0
        if (trans_tab[i,j]>middle) and (trans_tab[i,j]<high):
            mem_values[i,(3*j)+0]=0
            mem_values[i,(3*j)+1]=(high-trans_tab[i,j])/(high-middle)
            mem_values[i,(3*j)+2]=1-mem_values[i,(3*j)+1]
        fuz_utility[i,(3*j)+0]=mem_values[i,(3*j)+0]*trans_tab[i,j]*profits[j]
        fuz_utility[i,(3*j)+1]=mem_values[i,(3*j)+1]*trans_tab[i,j]*profits[j]
        fuz_utility[i,(3*j)+2]=mem_values[i,(3*j)+2]*trans_tab[i,j]*profits[j]
        
mtfu=np.zeros(nt,dtype='float')        
for i in range(0,nt):
    summ=0.0
    for j in range(0,ni):
        maxx=fuz_utility[i,(3*j)+0]
        if maxx<fuz_utility[i,(3*j)+1]:
            maxx=fuz_utility[i,(3*j)+1]
        if maxx<fuz_utility[i,(3*j)+2]:
            maxx=fuz_utility[i,(3*j)+2]
        summ=summ+maxx
    mtfu[i]=summ
    
#High Utility Itemsets 
#1-Itemset
lambd=25
mtfu_oneitemset=np.zeros((ni*3),dtype='float')
for i in range(0,(ni*3)):
    a=np.nonzero(fuz_utility[:,i])
    a=np.array(a)
    s=np.size(a)
    mtfu=np.array(mtfu)
    for j in range(0,s):
        mtfu_oneitemset[i]=mtfu_oneitemset[i]+mtfu[a[:,j]]
hfuub_1=np.array(np.nonzero(mtfu_oneitemset>lambd));
numbers=np.shape(hfuub_1);
numbers=numbers[1];
num=np.zeros(numbers,dtype='int');
k=0;
for i in range(0,np.size(mtfu_oneitemset)):
    if mtfu_oneitemset[i]>lambd:
        num[k]=i;
        k=k+1;
if numbers==0:
    print("No high utility itemsets");
else:
    import itertools
    final=list();
    for i in range(0,(3*ni)):
        util=0;
        for j in range(0,nt):
            util=util+mem_values[j][i]*profits[int(i/3)]*trans_tab[j][int(i/3)];
        if util>lambd:
            final.append(i);
    for i in range(2,k):
        combi=list(itertools.combinations(num,i));
        flag=0;
        for j in range(0,len(combi)):
            c=np.zeros(i,dtype='int');
            e=np.zeros(i,dtype='int');
            for k in range(0,i):
                c[k]=(combi[j][k])/3;
                e[k]=combi[j][k];
            d=np.unique(c);
            if np.size(c)==np.size(d):
               summx=0;
               for l in range(0,nt):
                   tempsum=min(mem_values[l][e]);
                   summx=summx+tempsum*sum(profits[(e/3)]*trans_tab[l][(e/3)]);
               print(summx);
               if summx>lambd:
                  final.append(e);
                  flag=flag+1;
        if flag==0:
            break;
                      
    print(final);
    
    
    
    
        

    



            

        
