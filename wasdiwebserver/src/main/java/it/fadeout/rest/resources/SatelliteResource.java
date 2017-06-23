package it.fadeout.rest.resources;

import it.fadeout.business.InstanceFinder;
import it.fadeout.viewmodels.SatelliteOrbitResultViewModel;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.nfs.orbits.sat.SatFactory;
import org.nfs.orbits.sat.Satellite;

import satLib.astro.time.Time;


@Path("/satellite")
public class SatelliteResource {

	
	@GET
	@Path("/track/{satellitename}")
	@Produces({"application/xml", "application/json", "text/html"})
	@Consumes(MediaType.APPLICATION_JSON)
	public SatelliteOrbitResultViewModel getSatelliteTrack(@HeaderParam("x-session-token") String sSessionId,@PathParam("satellitename")String satname){
		SatelliteOrbitResultViewModel ret = new SatelliteOrbitResultViewModel();
		String satres=InstanceFinder.s_sOrbitSatsMap.get(satname);
		try {
		
			Time tconv = new Time();
			double k=180.0/Math.PI;
		    tconv.setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
			Satellite sat = SatFactory.buildSat(satres);
			ret.satelliteName=sat.getDescription();
			sat.getSatController().moveToNow();
			ret.currentPosition=(sat.getOrbitCore().getLatitude()*k)+";"+(sat.getOrbitCore().getLongitude()*k)+";"+sat.getOrbitCore().getAltitude();
			sat.getOrbitCore().setShowGroundTrack(true);
			//lead
			double[] tm = sat.getOrbitCore().getTimeLead();
			int num=sat.getOrbitCore().getNumGroundTrackLeadPts();
			for(int i=0; i<num; i++){
				ret.nextPositions.add(
						(sat.getOrbitCore().getGroundTrackLlaLeadPt(i)[0]*k)+";"+
						(sat.getOrbitCore().getGroundTrackLlaLeadPt(i)[1]*k)+";"+
						sat.getOrbitCore().getGroundTrackLlaLeadPt(i)[2]+";"
						);
				ret.nextPositionsTime.add(tconv.convertJD2String(tm[i]));
			}
			//lag
			tm = sat.getOrbitCore().getTimeLag();
			num=sat.getOrbitCore().getNumGroundTrackLagPts();
			for(int i=0; i<num; i++){
				ret.lastPositions.add(
						(sat.getOrbitCore().getGroundTrackLlaLagPt(i)[0]*k)+";"+
						(sat.getOrbitCore().getGroundTrackLlaLagPt(i)[1]*k)+";"+
						sat.getOrbitCore().getGroundTrackLlaLagPt(i)[2]+";"
						);
				ret.lastPositionsTime.add(tconv.convertJD2String(tm[i]));
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ret;
	}
}