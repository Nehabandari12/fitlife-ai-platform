import React, { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router';
import { getActivityDetail, deleteActivity, getActivityRecommendation, updateActivity } from '../services/api';
import { ArrowLeft, Calendar, Clock, Flame, TrendingUp, AlertCircle, Lightbulb, ShieldCheck, Droplet } from 'lucide-react';

const ActivityDetail = () => {

  const { id } = useParams();
  const navigate = useNavigate();
  const [activity, setActivity] = useState(null);
  const [recommendation, setRecommendation] = useState(null);
  const [loadingActivity, setLoadingActivity] = useState(true);
  const [loadingRecommendation, setLoadingRecommendation] = useState(true);
  const [recError, setRecError] = useState(null);

  const [isEditing, setIsEditing] = useState(false);
  const [editData, setEditData] = useState({
    type: '',
    duration: '',
    caloriesBurned: '',
    timeOfDay: '',
    mealTiming: '',
    waterIntakeMl: ''
  });

  const formatTimeOfDay = (value) => {
    switch (value) {
      case 'MORNING': return 'Morning';
      case 'AFTERNOON': return 'Afternoon';
      case 'EVENING': return 'Evening';
      case 'NIGHT': return 'Night';
      default: return value || 'Not specified';
    }
  };

  const formatMealTiming = (value) => {
    switch (value) {
      case 'BEFORE_LUNCH': return 'Before lunch';
      case 'AFTER_LUNCH': return 'After lunch';
      default: return value || 'Not specified';
    }
  };

  useEffect(() => {
    const fetchActivityDetail = async () => {
      try {
        setLoadingActivity(true);
        const response = await getActivityDetail(id); // /activities/{id}
        setActivity(response.data);
      } catch (error) {
        console.error(error);
      } finally {
        setLoadingActivity(false);
      }
    }

    const fetchRecommendation = async () => {
      try {
        setLoadingRecommendation(true);
        const response = await getActivityRecommendation(id); // /recommendations/activity/{id}
        setRecommendation(response.data);
        setRecError(null);
      } catch (error) {
        if (error.response && error.response.status === 404) {
          setRecError('No AI recommendation generated yet for this activity.');
        } else {
          console.error('Error fetching recommendation:', error);
          setRecError('Failed to load AI recommendation.');
        }
      } finally {
        setLoadingRecommendation(false);
      }
    };

    fetchActivityDetail();
    fetchRecommendation();
  }, [id]);

  // When activity is loaded, prefill edit form
  useEffect(() => {
    if (activity) {
      const metrics = activity.additionalMetrics || {};
      setEditData({
        type: activity.type || '',
        duration: activity.duration ?? '',
        caloriesBurned: activity.caloriesBurned ?? '',
        timeOfDay: metrics.timeOfDay || '',
        mealTiming: metrics.mealTiming || '',
        waterIntakeMl: metrics.waterIntakeMl ?? ''
      });
    }
  }, [activity]);

  const handleDelete = async () => {
    if (!window.confirm("Are you sure you want to delete this activity?")) return;

    try {
      await deleteActivity(id);
      alert("Activity deleted successfully");
      navigate("/");
    } catch (error) {
      if (error.response) {
        console.error(
          "Delete failed with response:",
          error.response.status,
          error.response.data
        );
      } else {
        console.error("Delete failed (network/CORS?):", error.message);
      }
      alert("Failed to delete activity");
    }
  };

  const handleUpdate = async (e) => {
    e.preventDefault();

    try {
      const updatedPayload = {
        type: editData.type,
        duration: Number(editData.duration),
        caloriesBurned: Number(editData.caloriesBurned),
        startTime: activity.startTime,
        additionalMetrics: {
          ...(activity.additionalMetrics || {}),
          timeOfDay: editData.timeOfDay,
          mealTiming: editData.mealTiming,
          waterIntakeMl: editData.waterIntakeMl
            ? Number(editData.waterIntakeMl)
            : null
        }
      };

      await updateActivity(id, updatedPayload);

      alert("Activity updated successfully. AI may regenerate the recommendation.");

      // Update local state so UI reflects changes
      setActivity(prev => ({
        ...prev,
        type: editData.type,
        duration: Number(editData.duration),
        caloriesBurned: Number(editData.caloriesBurned),
        additionalMetrics: {
          ...(prev.additionalMetrics || {}),
          timeOfDay: editData.timeOfDay,
          mealTiming: editData.mealTiming,
          waterIntakeMl: editData.waterIntakeMl
            ? Number(editData.waterIntakeMl)
            : null
        }
      }));

      setIsEditing(false);
    } catch (error) {
      if (error.response) {
        console.error(
          "Update failed with response:",
          error.response.status,
          error.response.data
        );
      } else {
        console.error("Update failed (network/CORS?):", error.message);
      }
      alert("Failed to update activity");
    }
  };

  const handleEditFieldChange = (field, value) => {
    setEditData(prev => ({ ...prev, [field]: value }));
  };

  if (loadingActivity || !activity) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-gray-900">
        <div className="text-center">
          <div className="w-16 h-16 border-4 border-blue-500 border-t-transparent rounded-full animate-spin mx-auto mb-4"></div>
          <p className="text-white text-xl">Loading activity details...</p>
        </div>
      </div>
    );
  }

  const metrics = activity.additionalMetrics || {};

  return (
    <div className="max-w-7xl mx-auto p-4 md:p-6">
      {/* Back Button */}
      <button
        onClick={() => navigate('/')}
        className="flex items-center gap-2 text-blue-400 hover:text-blue-300 mb-6 transition group"
      >
        <ArrowLeft size={20} className="group-hover:-translate-x-1 transition-transform" />
        <span className="font-medium">Back to Home</span>
      </button>

      {/* Activity Details Card */}
      <div className="bg-gradient-to-br from-gray-800 to-gray-900 rounded-2xl border border-gray-700 p-8 mb-6 shadow-2xl">
        <h1 className="text-3xl font-bold text-white mb-6 flex items-center gap-3">
          <div className="w-12 h-12 bg-blue-500/20 rounded-full flex items-center justify-center">
            <Flame className="text-blue-400" size={24} />
          </div>
          Activity Details
        </h1>
        
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* Activity Type */}
          <div className="bg-gray-700/50 rounded-lg p-5 border border-gray-600">
            <div className="flex items-center gap-3 mb-2">
              <TrendingUp className="text-blue-400" size={20} />
              <p className="text-gray-400 text-sm font-medium">Activity Type</p>
            </div>
            <p className="text-2xl font-bold text-white">{activity.type}</p>
          </div>

          {/* Duration */}
          <div className="bg-gray-700/50 rounded-lg p-5 border border-gray-600">
            <div className="flex items-center gap-3 mb-2">
              <Clock className="text-green-400" size={20} />
              <p className="text-gray-400 text-sm font-medium">Duration</p>
            </div>
            <p className="text-2xl font-bold text-white">
              {activity.duration}{' '}
              <span className="text-lg text-gray-400">minutes</span>
            </p>
          </div>

          {/* Calories Burned */}
          <div className="bg-gray-700/50 rounded-lg p-5 border border-gray-600">
            <div className="flex items-center gap-3 mb-2">
              <Flame className="text-orange-400" size={20} />
              <p className="text-gray-400 text-sm font-medium">Calories Burned</p>
            </div>
            <p className="text-2xl font-bold text-white">
              {activity.caloriesBurned}{' '}
              <span className="text-lg text-gray-400">kcal</span>
            </p>
          </div>

          {/* Date */}
          <div className="bg-gray-700/50 rounded-lg p-5 border border-gray-600">
            <div className="flex items-center gap-3 mb-2">
              <Calendar className="text-purple-400" size={20} />
              <p className="text-gray-400 text-sm font-medium">Date</p>
            </div>
            <p className="text-lg font-semibold text-white">
              {activity.createdAt
                ? new Date(activity.createdAt).toLocaleString()
                : 'N/A'}
            </p>
          </div>

          {/* Time of Day */}
          <div className="bg-gray-700/50 rounded-lg p-5 border border-gray-600">
            <div className="flex items-center gap-3 mb-2">
              <Clock className="text-blue-300" size={20} />
              <p className="text-gray-400 text-sm font-medium">Time of Day</p>
            </div>
            <p className="text-xl font-semibold text-white">
              {formatTimeOfDay(metrics.timeOfDay)}
            </p>
          </div>

          {/* Meal Timing */}
          <div className="bg-gray-700/50 rounded-lg p-5 border border-gray-600">
            <div className="flex items-center gap-3 mb-2">
              <Flame className="text-pink-300" size={20} />
              <p className="text-gray-400 text-sm font-medium">Around Lunch</p>
            </div>
            <p className="text-xl font-semibold text-white">
              {formatMealTiming(metrics.mealTiming)}
            </p>
          </div>

          {/* Water Intake */}
          <div className="bg-gray-700/50 rounded-lg p-5 border border-gray-600">
            <div className="flex items-center gap-3 mb-2">
              <Droplet className="text-cyan-300" size={20} />
              <p className="text-gray-400 text-sm font-medium">Water Intake</p>
            </div>
            <p className="text-xl font-semibold text-white">
              {metrics.waterIntakeMl != null && metrics.waterIntakeMl !== ''
                ? `${metrics.waterIntakeMl} ml`
                : 'Not specified'}
            </p>
          </div>
        </div>

        <div className="mt-6 flex gap-3">
          <button
            onClick={() => setIsEditing(true)}
            className="bg-yellow-500 hover:bg-yellow-600 text-black px-6 py-3 rounded-lg font-medium transition"
          >
            Edit Activity
          </button>

          <button
            onClick={handleDelete}
            className="bg-red-600 hover:bg-red-700 text-white px-6 py-3 rounded-lg font-medium transition"
          >
            Delete Activity
          </button>
        </div>

        {/* Edit Form */}
        {isEditing && (
          <div className="bg-gray-800 border border-gray-700 rounded-2xl p-6 mt-6">
            <h2 className="text-2xl font-bold text-white mb-4">Edit Activity</h2>
            <form onSubmit={handleUpdate} className="space-y-4">
              {/* Type */}
              <div>
                <label className="block text-gray-300 mb-1">Activity Type</label>
                <select
                  value={editData.type}
                  onChange={(e) => handleEditFieldChange('type', e.target.value)}
                  className="w-full bg-gray-700 text-white p-3 rounded-lg border border-gray-600"
                >
                  <option value="RUNNING">Running</option>
                  <option value="CARDIO">Cardio</option>
                  <option value="SWIMMING">Swimming</option>
                  <option value="WALKING">Walking</option>
                  <option value="CYCLING">Cycling</option>
                  <option value="YOGA">Yoga</option>
                  <option value="HIIT">HIIT</option>
                  <option value="WEIGHT_TRAINING">Weight Training</option>
                  <option value="STRETCHING">Stretching</option>
                  <option value="OTHER">Other</option>
                </select>
              </div>

              {/* Duration */}
              <div>
                <label className="block text-gray-300 mb-1">Duration (minutes)</label>
                <input
                  type="number"
                  value={editData.duration}
                  onChange={(e) => handleEditFieldChange('duration', e.target.value)}
                  className="w-full bg-gray-700 text-white p-3 rounded-lg border border-gray-600"
                />
              </div>

              {/* Calories */}
              <div>
                <label className="block text-gray-300 mb-1">Calories Burned</label>
                <input
                  type="number"
                  value={editData.caloriesBurned}
                  onChange={(e) => handleEditFieldChange('caloriesBurned', e.target.value)}
                  className="w-full bg-gray-700 text-white p-3 rounded-lg border border-gray-600"
                />
              </div>

              {/* Time of Day */}
              <div>
                <label className="block text-gray-300 mb-1">Time of Day</label>
                <select
                  value={editData.timeOfDay}
                  onChange={(e) => handleEditFieldChange('timeOfDay', e.target.value)}
                  className="w-full bg-gray-700 text-white p-3 rounded-lg border border-gray-600"
                >
                  <option value="">Select time of day</option>
                  <option value="MORNING">Morning</option>
                  <option value="AFTERNOON">Afternoon</option>
                  <option value="EVENING">Evening</option>
                  <option value="NIGHT">Night</option>
                </select>
              </div>

              {/* Meal Timing */}
              <div>
                <label className="block text-gray-300 mb-1">Before/After Lunch</label>
                <select
                  value={editData.mealTiming}
                  onChange={(e) => handleEditFieldChange('mealTiming', e.target.value)}
                  className="w-full bg-gray-700 text-white p-3 rounded-lg border border-gray-600"
                >
                  <option value="">Select option</option>
                  <option value="BEFORE_LUNCH">Before lunch</option>
                  <option value="AFTER_LUNCH">After lunch</option>
                </select>
              </div>

              {/* Water Intake */}
              <div>
                <label className="block text-gray-300 mb-1">Water Intake (ml)</label>
                <input
                  type="number"
                  min="0"
                  value={editData.waterIntakeMl}
                  onChange={(e) => handleEditFieldChange('waterIntakeMl', e.target.value)}
                  className="w-full bg-gray-700 text-white p-3 rounded-lg border border-gray-600"
                />
              </div>

              <div className="flex gap-3 mt-4">
                <button
                  type="submit"
                  className="bg-green-600 hover:bg-green-700 text-white px-6 py-3 rounded-lg font-medium transition"
                >
                  Save Changes
                </button>
                <button
                  type="button"
                  onClick={() => setIsEditing(false)}
                  className="text-gray-300 hover:text-gray-100 px-4 py-3"
                >
                  Cancel
                </button>
              </div>
            </form>
          </div>
        )}
      </div>

      {/* AI Recommendation Card (unchanged logic, but now uses new metrics in backend prompt) */}
      <div className="bg-gradient-to-br from-gray-800 to-gray-900 rounded-2xl border border-gray-700 p-8 shadow-2xl">
        <h2 className="text-3xl font-bold text-white mb-6 flex items-center gap-3">
          <div className="w-12 h-12 bg-purple-500/20 rounded-full flex items-center justify-center">
            <Lightbulb className="text-purple-400" size={24} />
          </div>
          AI Recommendation
        </h2>

        {loadingRecommendation && (
          <p className="text-gray-400">Loading recommendation...</p>
        )}

        {!loadingRecommendation && recError && (
          <div className="flex items-center gap-2 bg-yellow-500/10 border border-yellow-500/30 text-yellow-300 px-4 py-3 rounded-lg mb-4">
            <AlertCircle size={20} />
            <span>{recError}</span>
          </div>
        )}

        {!loadingRecommendation && recommendation && (
          <>
            {recommendation.status === 'FALLBACK' && (
              <div className="mb-6 flex items-center gap-2 text-yellow-300 bg-yellow-900/20 border border-yellow-700 rounded-lg p-4">
                <AlertCircle size={20} />
                <span>
                  The AI analysis was unavailable ({recommendation.fallbackReason}), so this is general guidance,
                  not an analysis of this activity.
                </span>
              </div>
            )}
            {/* Analysis Section */}
            <div className="mb-8">
              <div className="flex items-center gap-2 mb-3">
                <TrendingUp className="text-blue-400" size={20} />
                <h3 className="text-xl font-bold text-blue-400">Analysis</h3>
              </div>
              <div className="bg-gray-700/30 rounded-lg p-5 border border-gray-600">
                <p className="text-gray-300 leading-relaxed whitespace-pre-line">
                  {recommendation.recommendation}
                </p>
              </div>
            </div>

            {/* Improvements */}
            {recommendation.improvements && recommendation.improvements.length > 0 && (
              <div className="mb-8">
                <div className="flex items-center gap-2 mb-3">
                  <AlertCircle className="text-yellow-400" size={20} />
                  <h3 className="text-xl font-bold text-yellow-400">Improvements</h3>
                </div>
                <div className="space-y-3">
                  {recommendation.improvements.map((imp, index) => (
                    <div
                      key={index}
                      className="flex gap-3 bg-yellow-500/10 rounded-lg p-4 border border-yellow-500/20"
                    >
                      <span className="text-yellow-400 font-bold">•</span>
                      <p className="text-gray-300 flex-1">{imp}</p>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Suggestions */}
            {recommendation.suggestions && recommendation.suggestions.length > 0 && (
              <div className="mb-8">
                <div className="flex items-center gap-2 mb-3">
                  <Lightbulb className="text-green-400" size={20} />
                  <h3 className="text-xl font-bold text-green-400">Suggestions</h3>
                </div>
                <div className="space-y-3">
                  {recommendation.suggestions.map((sug, index) => (
                    <div
                      key={index}
                      className="flex gap-3 bg-green-500/10 rounded-lg p-4 border border-green-500/20"
                    >
                      <span className="text-green-400 font-bold">•</span>
                      <p className="text-gray-300 flex-1">{sug}</p>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Safety */}
            {recommendation.safety && recommendation.safety.length > 0 && (
              <div>
                <div className="flex items-center gap-2 mb-3">
                  <ShieldCheck className="text-red-400" size={20} />
                  <h3 className="text-xl font-bold text-red-400">Safety Guidelines</h3>
                </div>
                <div className="space-y-3">
                  {recommendation.safety.map((s, index) => (
                    <div
                      key={index}
                      className="flex gap-3 bg-red-500/10 rounded-lg p-4 border border-red-500/20"
                    >
                      <span className="text-red-400 font-bold">•</span>
                      <p className="text-gray-300 flex-1">{s}</p>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}

export default ActivityDetail;
