import { getAsync, postAsync, putAsync, deleteAsync } from '../helpers/apiHelper'

const endpoint = "/api/rtc";

class RtcApiService {
    constructor() {

    }
    async postDescAsync(model) {
        return await postAsync(endpoint+"/desc", model);
    }
    async postCandAsync(model){
        return await postAsync(endpoint+"/cand", model);
    }
}

export default new RtcApiService()