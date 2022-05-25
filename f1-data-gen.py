import time
import fastf1
import asyncio
from ably import AblyRest

async def main():
    async with AblyRest('973y9Q.GV9S4g:x-SDde68V2RiCvQqZP14sTo_S6U89L6etFtz769S04U') as ably:
        lapChannel = ably.channels.get("lap")
        speedChannel = ably.channels.get("speed")
        fastf1.Cache.enable_cache('/Users/adamnieves/Documents/fastf1')  
        session = fastf1.get_session(2021, 'Spanish Grand Prix', 'Q')
        session.load()
        
        ham_laps = session.laps.pick_driver('HAM')
        print('Hamilton Laps ' + str(ham_laps))
        ham_car_data = ham_laps.get_car_data()
        print('Hamilton car data ' + str(ham_car_data))
        ham_speed = ham_car_data['Speed']
        print('Hamilton speed ' + str(ham_speed))

        #prime lap times
        lapTime = str(ham_laps['LapTime'][0])
        await lapChannel.publish('lap', lapTime[-15:])
        lapTime = str(ham_laps['LapTime'][1])
        await lapChannel.publish('lap', lapTime[-15:])

        for i in range(len(ham_laps)): #len(ham_laps)
            #for j in range(len(ham_laps['Time'])):
            ham_car_data = ham_laps.get_car_data()
            ham_speed = ham_car_data['Speed']
            for k in range(150): #150
                await speedChannel.publish('speed', str(ham_speed[k]))
                print('Hello from Speed loop ' + speedChannel.name + ' ' + str(ham_speed[k]))
                time.sleep(.2)
            lapTime = str(ham_laps['LapTime'][i])
            await lapChannel.publish('lap', lapTime[-15:])
            print('Hello from Lap loop ' + lapChannel.name + ' ' + lapTime[-15:])

if __name__ == '__main__':
    asyncio.run(main())